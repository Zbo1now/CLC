package com.campuscoin.service;

import com.campuscoin.dao.AchievementDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.AchievementSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementDao achievementDao;
    private final BaiduService baiduService;
    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public AchievementService(AchievementDao achievementDao,
                              BaiduService baiduService,
                              TeamDao teamDao,
                              TransactionService transactionService) {
        this.achievementDao = achievementDao;
        this.baiduService = baiduService;
        this.teamDao = teamDao;
        this.transactionService = transactionService;
    }

    public AchievementSubmission submit(int teamId, String category, String subType, String title, String description, String proofBase64) {
        if (teamId <= 0) {
            throw new IllegalArgumentException("参数错误: teamId");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("请选择成果类型");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写名称/标题");
        }
        if (description == null) {
            description = "";
        }
        if (proofBase64 == null || proofBase64.trim().isEmpty()) {
            throw new IllegalArgumentException("请上传证明材料");
        }

        String cleanCategory = category.trim().toUpperCase();
        String cleanSubType = (subType == null || subType.trim().isEmpty()) ? null : subType.trim().toUpperCase();

        String ext = ".jpg";
        if (proofBase64.startsWith("data:image/png")) {
            ext = ".png";
        } else if (proofBase64.startsWith("data:image/jpeg") || proofBase64.startsWith("data:image/jpg")) {
            ext = ".jpg";
        } else if (proofBase64.startsWith("data:application/pdf")) {
            ext = ".pdf";
        }

        String fileName = "achievements/" + teamId + "_" + System.currentTimeMillis() + ext;
        String proofUrl = baiduService.uploadToBos(proofBase64, fileName);
        if (proofUrl == null) {
            logger.warn("成果提交失败: BOS上传失败 teamId={}, category={}", teamId, cleanCategory);
            throw new IllegalStateException("证明材料上传失败，请重试");
        }

        AchievementSubmission sub = new AchievementSubmission();
        sub.setTeamId(teamId);
        sub.setCategory(cleanCategory);
        sub.setSubType(cleanSubType);
        sub.setTitle(title.trim());
        sub.setDescription(description.trim());
        sub.setProofUrl(proofUrl);
        sub.setStatus("PENDING");

        achievementDao.create(sub);
        logger.info("成果提交成功: id={}, teamId={}, category={}, subType={}", sub.getId(), teamId, cleanCategory, cleanSubType);
        return achievementDao.findById(sub.getId());
    }

    public List<AchievementSubmission> listMine(int teamId) {
        return achievementDao.listByTeam(teamId);
    }

    public AchievementSubmission getMine(int teamId, int id) {
        AchievementSubmission sub = achievementDao.findById(id);
        if (sub == null || sub.getTeamId() != teamId) {
            return null;
        }
        return sub;
    }

    public List<AchievementSubmission> adminList(String status) {
        String s = (status == null || status.trim().isEmpty()) ? null : status.trim().toUpperCase();
        return achievementDao.listAll(s);
    }

    @Transactional
    public AchievementSubmission adminApprove(int id, String reviewedBy) {
        AchievementSubmission locked = achievementDao.findByIdForUpdate(id);
        if (locked == null) {
            throw new IllegalArgumentException("成果不存在");
        }
        if (!"PENDING".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可审核");
        }

        int reward = calculateRewardCoins(locked.getCategory(), locked.getSubType());
        int rows = achievementDao.approve(id, reward, reviewedBy);
        if (rows != 1) {
            throw new IllegalStateException("审核失败，请刷新后重试");
        }

        // 发放奖励：余额增加 + 写流水
        teamDao.addBalance(locked.getTeamId(), reward);
        try {
            String desc = buildRewardDesc(locked);
            transactionService.record(locked.getTeamId(), "ACHIEVEMENT_REWARD", reward, desc);
        } catch (Exception e) {
            logger.error("成果奖励流水记录失败: teamId={}, achId={}, reward={}", locked.getTeamId(), id, reward, e);
        }

        logger.info("成果审核通过: id={}, teamId={}, reward={}, reviewedBy={}", id, locked.getTeamId(), reward, reviewedBy);
        return achievementDao.findById(id);
    }

    @Transactional
    public AchievementSubmission adminReject(int id, String reviewedBy, String rejectReason) {
        AchievementSubmission locked = achievementDao.findByIdForUpdate(id);
        if (locked == null) {
            throw new IllegalArgumentException("成果不存在");
        }
        if (!"PENDING".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可审核");
        }

        String reason = (rejectReason == null || rejectReason.trim().isEmpty()) ? "资料不符合要求" : rejectReason.trim();
        int rows = achievementDao.reject(id, reason, reviewedBy);
        if (rows != 1) {
            throw new IllegalStateException("审核失败，请刷新后重试");
        }

        logger.info("成果审核驳回: id={}, teamId={}, reviewedBy={}, reason={}", id, locked.getTeamId(), reviewedBy, reason);
        return achievementDao.findById(id);
    }

    private int calculateRewardCoins(String category, String subType) {
        String c = (category == null) ? "" : category.trim().toUpperCase();
        String s = (subType == null) ? "" : subType.trim().toUpperCase();

        switch (c) {
            case "PAPER":
                return 100;
            case "PATENT":
                if ("GRANTED".equals(s) || "AUTHORIZED".equals(s)) {
                    return 400;
                }
                if ("ACCEPTED".equals(s)) {
                    return 200;
                }
                return 200;
            case "COMPETITION":
                if ("NATIONAL".equals(s)) {
                    return 300;
                }
                if ("PROVINCE".equals(s) || "PROVINCIAL".equals(s)) {
                    return 150;
                }
                if ("SCHOOL".equals(s)) {
                    return 50;
                }
                return 80;
            case "OTHER":
                return 80;
            default:
                return 80;
        }
    }

    private String buildRewardDesc(AchievementSubmission sub) {
        String c = sub.getCategory() == null ? "" : sub.getCategory();
        String s = sub.getSubType() == null ? "" : sub.getSubType();

        String name;
        if ("PAPER".equalsIgnoreCase(c)) {
            name = "学术论文";
        } else if ("PATENT".equalsIgnoreCase(c)) {
            name = "专利" + (s.isEmpty() ? "" : ("ACCEPTED".equalsIgnoreCase(s) ? "(受理)" : "(授权)"));
        } else if ("COMPETITION".equalsIgnoreCase(c)) {
            String level;
            if ("SCHOOL".equalsIgnoreCase(s)) {
                level = "校级";
            } else if ("PROVINCE".equalsIgnoreCase(s) || "PROVINCIAL".equalsIgnoreCase(s)) {
                level = "省级";
            } else if ("NATIONAL".equalsIgnoreCase(s)) {
                level = "国家级";
            } else {
                level = "";
            }
            name = "竞赛获奖" + (level.isEmpty() ? "" : ("(" + level + ")"));
        } else {
            name = "其他创新成果";
        }

        return "成果审核通过奖励 - " + name + " - " + sub.getTitle();
    }
}
