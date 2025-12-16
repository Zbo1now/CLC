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
                e.setMyParticipationStatus(cleanStatus(p.getStatus()));
            } else {
                e.setMyParticipationId(null);
                e.setMyParticipationStatus(null);
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
                e.setMyParticipationId(p.getId());
                e.setMyParticipationStatus(cleanStatus(p.getStatus()));
            }
        } catch (Exception ex) {
            logger.warn("查询培训活动详情的我的申报失败: teamId={}, eventId={}", teamId, eventId, ex);
        }

        return e;
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
        if (e.getPublishStatus() != null && !"PUBLISHED".equalsIgnoreCase(e.getPublishStatus().trim())) {
            throw new IllegalStateException("活动未发布");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = e.getEndTime() != null ? e.getEndTime().toLocalDateTime() : null;
        if (end != null && now.isBefore(end)) {
            throw new IllegalStateException("活动未结束，暂不能提交证明");
        }

        TrainingParticipation existing = trainingParticipationDao.findByEventAndTeam(eventId, teamId);
        if (existing != null) {
            throw new IllegalStateException("你已提交过该活动的申报");
        }

        String ext = guessExt(proofBase64);
        String fileName = "training/" + teamId + "_" + eventId + "_" + System.currentTimeMillis() + ext;
        String proofUrl = baiduService.uploadToBos(proofBase64, fileName);
        if (proofUrl == null) {
            logger.warn("培训申报失败: BOS上传失败 teamId={}, eventId={}", teamId, eventId);
            throw new IllegalStateException("证明材料上传失败，请重试");
        }

        TrainingParticipation p = new TrainingParticipation();
        p.setEventId(eventId);
        p.setTeamId(teamId);
        p.setProofUrl(proofUrl);
        p.setNote(note.trim());
        p.setStatus("PENDING");
        trainingParticipationDao.create(p);

        logger.info("培训申报提交成功: teamId={}, eventId={}, participationId={}", teamId, eventId, p.getId());
        return trainingParticipationDao.findByEventAndTeam(eventId, teamId);
    }

    public List<TrainingParticipationView> listMyParticipations(int teamId) {
        return trainingParticipationDao.listMyViews(teamId);
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
