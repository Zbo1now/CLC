package com.campuscoin.service;

import com.campuscoin.dao.TrainingEventDao;
import com.campuscoin.dao.TrainingParticipationDao;
import com.campuscoin.model.TrainingEvent;
import com.campuscoin.model.TrainingParticipation;
import com.campuscoin.model.TrainingParticipationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TrainingService {

    /**
     * 从completion_notes/note字段中解析出“证明材料”url
     */
    private String extractProofUrlFromNote(String note) {
        if (note == null) return null;
        String marker = "证明材料：";
        int idx = note.indexOf(marker);
        if (idx < 0) return null;
        String rest = note.substring(idx + marker.length());
        int endIdx = rest.indexOf("\n");
        String url = (endIdx == -1 ? rest : rest.substring(0, endIdx)).trim();
        if (url.isEmpty() || url.startsWith("备注：")) return null;
        return url;
    }

    /**
     * 统一规范BOS对象key前缀，防止前缀混乱。
     * 只允许training/前缀，自动去除多余/错误前缀。
     */
    private String normalizeKey(String key) {
        if (key == null) return null;
        String k = key.trim();
        // 去除开头的/和常见错误前缀
        while (k.startsWith("/")) k = k.substring(1);
        if (k.startsWith("faces/")) k = k.substring(6);
        if (k.startsWith("achievements/")) k = k.substring(13);
        if (!k.startsWith("training/")) k = "training/" + k;
        return k;
    }

    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);

    private final TrainingEventDao trainingEventDao;
    private final TrainingParticipationDao trainingParticipationDao;
    private final BaiduService baiduService;

    public TrainingService(TrainingEventDao trainingEventDao,
                           TrainingParticipationDao trainingParticipationDao,
                           BaiduService baiduService) {
        this.trainingEventDao = trainingEventDao;
        this.trainingParticipationDao = trainingParticipationDao;
        this.baiduService = baiduService;
    }

    public List<TrainingEvent> listForTeam(int teamId) {
        List<TrainingEvent> events = trainingEventDao.listPublished();
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> ids = new ArrayList<>();
        for (TrainingEvent e : events) {
            if (e != null && e.getId() != null) {
                ids.add(e.getId());
            }
        }

        Map<Integer, TrainingParticipation> mine = new HashMap<>();
        try {
            List<TrainingParticipation> list = trainingParticipationDao.listByTeamAndEventIds(teamId, ids);
            if (list != null) {
                for (TrainingParticipation p : list) {
                    if (p != null && p.getEventId() != null) {
                        mine.put(p.getEventId(), p);
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("查询我的培训申报失败: teamId={}", teamId, ex);
        }

        LocalDateTime now = LocalDateTime.now();
        for (TrainingEvent e : events) {
            if (e == null) {
                continue;
            }
            LocalDateTime st = e.getStartTime() != null ? e.getStartTime().toLocalDateTime() : null;
            LocalDateTime et = e.getEndTime() != null ? e.getEndTime().toLocalDateTime() : null;
            e.setEventStatus(computeEventStatus(now, st, et));

            TrainingParticipation p = e.getId() != null ? mine.get(e.getId()) : null;
            if (p != null) {
                e.setMyParticipationId(p.getId());
                if (!hasSubmittedProof(p)) {
                    // 只报名未提交材料
                    e.setMyParticipationStatus("SIGNED_UP");
                } else {
                    // 提交材料后才有审核状态
                    e.setMyParticipationStatus(cleanStatus(p.getStatus()));
                }
                e.setMyProofSubmitted(hasSubmittedProof(p));
            } else {
                e.setMyParticipationId(null);
                e.setMyParticipationStatus(null);
                e.setMyProofSubmitted(false);
            }
        }

        return events;
    }

    public TrainingEvent getEventForTeam(int teamId, int eventId) {
        TrainingEvent e = trainingEventDao.findById(eventId);
        if (e == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime st = e.getStartTime() != null ? e.getStartTime().toLocalDateTime() : null;
        LocalDateTime et = e.getEndTime() != null ? e.getEndTime().toLocalDateTime() : null;
        e.setEventStatus(computeEventStatus(now, st, et));

        try {
            TrainingParticipation p = trainingParticipationDao.findByEventAndTeam(eventId, teamId);
            if (p != null) {
                // 自动从note字段解析材料url并转为公有读域名型
                String proofUrl = extractProofUrlFromNote(p.getNote());
                p.setProofUrl(baiduService.toBosPublicUrl(proofUrl));
                e.setMyParticipationId(p.getId());
                e.setMyParticipationStatus(cleanStatus(p.getStatus()));
                e.setMyProofSubmitted(hasSubmittedProof(p));
            } else {
                e.setMyProofSubmitted(false);
            }
        } catch (Exception ex) {
            logger.warn("查询培训活动详情的我的申报失败: teamId={}, eventId={}", teamId, eventId, ex);
        }
        return e;
    }

    public TrainingParticipation signup(int teamId, int eventId) {
        if (teamId <= 0) {
            throw new IllegalArgumentException("参数错误: teamId");
        }
        if (eventId <= 0) {
            throw new IllegalArgumentException("参数错误: eventId");
        }

        TrainingEvent e = trainingEventDao.findById(eventId);
        if (e == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        if (e.getPublishStatus() != null && "CANCELLED".equalsIgnoreCase(e.getPublishStatus().trim())) {
            throw new IllegalStateException("活动已取消");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = e.getEndTime() != null ? e.getEndTime().toLocalDateTime() : null;
        if (end != null && !now.isBefore(end)) {
            throw new IllegalStateException("活动已结束，无法报名");
        }

        TrainingParticipation existing = trainingParticipationDao.findByEventAndTeam(eventId, teamId);
        if (existing != null) {
            throw new IllegalStateException("你已报名/申报过该活动");
        }

        int max = e.getMaxParticipants() == null ? 0 : e.getMaxParticipants();
        if (max > 0) {
            int cnt = trainingParticipationDao.countActiveByEvent(eventId);
            if (cnt >= max) {
                throw new IllegalStateException("报名人数已满");
            }
        }

        TrainingParticipation p = new TrainingParticipation();
        p.setEventId(eventId);
        p.setTeamId(teamId);
        trainingParticipationDao.createSignup(p);
        // 报名成功后，活动人数自增
        trainingEventDao.incrementCurrentParticipants(eventId);
        return trainingParticipationDao.findByEventAndTeam(eventId, teamId);
    }

    public TrainingParticipation submitParticipation(int teamId, int eventId, String proofBase64, String note) {
        if (teamId <= 0) {
            throw new IllegalArgumentException("参数错误: teamId");
        }
        if (eventId <= 0) {
            throw new IllegalArgumentException("参数错误: eventId");
        }
        if (proofBase64 == null || proofBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("请上传证明材料");
        }
        if (note == null) {
            note = "";
        }

        TrainingEvent e = trainingEventDao.findById(eventId);
        if (e == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        // 只要活动未取消即可
        if (e.getPublishStatus() != null && "CANCELLED".equalsIgnoreCase(e.getPublishStatus().trim())) {
            throw new IllegalStateException("活动已取消");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = e.getEndTime() != null ? e.getEndTime().toLocalDateTime() : null;
        if (end != null && now.isBefore(end)) {
            throw new IllegalStateException("活动未结束，暂不能提交证明");
        }

        TrainingParticipation existing = trainingParticipationDao.findByEventAndTeam(eventId, teamId);
        // 必须已报名才能提交材料
        if (existing == null) {
            throw new IllegalStateException("请先报名该活动，才能提交材料");
        }
        if (hasSubmittedProof(existing)) {
            throw new IllegalStateException("你已提交过该活动的申报");
        }

        String ext = guessExt(proofBase64);
        String rawKey = teamId + "_" + eventId + "_" + System.currentTimeMillis() + ext;
        String fileName = normalizeKey(rawKey);
        String proofUrl = baiduService.uploadToBos(proofBase64, fileName);
        if (proofUrl == null) {
            logger.warn("培训申报失败: BOS上传失败 teamId={}, eventId={}", teamId, eventId);
            throw new IllegalStateException("证明材料上传失败，请重试");
        }

        trainingParticipationDao.updateSubmission(eventId, teamId, proofUrl, note.trim());
        logger.info("培训申报提交成功: teamId={}, eventId={}, participationId={}", teamId, eventId, existing.getId());
        return trainingParticipationDao.findByEventAndTeam(eventId, teamId);
    }

    private boolean hasSubmittedProof(TrainingParticipation p) {
        if (p == null) {
            return false;
        }
        String note = p.getNote();
        if (note == null) {
            return false;
        }
        String marker = "证明材料：";
        int idx = note.indexOf(marker);
        if (idx < 0) {
            return false;
        }
        String rest = note.substring(idx + marker.length()).trim();
        if (rest.isEmpty()) {
            return false;
        }
        if (rest.startsWith("null")) {
            return false;
        }
        if (rest.startsWith("备注：")) {
            return false;
        }
        return true;
    }

    public List<TrainingParticipationView> listMyParticipations(int teamId) {
        List<TrainingParticipationView> list = trainingParticipationDao.listMyViews(teamId);
        if (list != null) {
            for (TrainingParticipationView v : list) {
                if (v != null) {
                    String proofUrl = extractProofUrlFromNote(v.getNote());
                    v.setProofUrl(baiduService.toBosPublicUrl(proofUrl));
                }
            }
        }
        return list;
    }

    private String computeEventStatus(LocalDateTime now, LocalDateTime start, LocalDateTime end) {
        if (start != null && now.isBefore(start)) {
            return "NOT_STARTED";
        }
        if (end != null && !now.isBefore(end)) {
            return "ENDED";
        }
        return "IN_PROGRESS";
    }

    private String cleanStatus(String status) {
        if (status == null) {
            return null;
        }
        String s = status.trim().toUpperCase();
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    private String guessExt(String base64) {
        String b = base64.trim();
        if (b.startsWith("data:image/png")) {
            return ".png";
        }
        if (b.startsWith("data:image/jpeg") || b.startsWith("data:image/jpg")) {
            return ".jpg";
        }
        if (b.startsWith("data:application/pdf")) {
            return ".pdf";
        }
        return ".jpg";
    }
}
