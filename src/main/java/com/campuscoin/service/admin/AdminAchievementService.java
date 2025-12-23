package com.campuscoin.service.admin;

import com.campuscoin.dao.AchievementDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.TransactionDao;
import com.campuscoin.dao.admin.AdminAchievementDao;
import com.campuscoin.dao.admin.AdminOperationLogDao;
import com.campuscoin.model.AchievementSubmission;
import com.campuscoin.model.Transaction;
import com.campuscoin.model.admin.AdminAchievementListItem;
import com.campuscoin.model.admin.AdminOperationLog;
import com.campuscoin.payload.PagedResponse;
import com.campuscoin.service.BaiduService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baidubce.services.bos.model.BosObject;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * 成果审核服务（管理员后台）
 */
@Service
public class AdminAchievementService {
    private static final Logger logger = LoggerFactory.getLogger(AdminAchievementService.class);

    private final AdminAchievementDao adminAchievementDao;
    private final AchievementDao achievementDao;
    private final TeamDao teamDao;
    private final TransactionDao transactionDao;
    private final AdminOperationLogDao adminOperationLogDao;

    private final BaiduService baiduService;

    @Autowired
    public AdminAchievementService(AdminAchievementDao adminAchievementDao,
                                  AchievementDao achievementDao,
                                  TeamDao teamDao,
                                  TransactionDao transactionDao,
                                  AdminOperationLogDao adminOperationLogDao,
                                  BaiduService baiduService) {
        this.adminAchievementDao = adminAchievementDao;
        this.achievementDao = achievementDao;
        this.teamDao = teamDao;
        this.transactionDao = transactionDao;
        this.adminOperationLogDao = adminOperationLogDao;
        this.baiduService = baiduService;
    }

    /**
     * 分页查询成果列表（带筛选条件）
     */
    public PagedResponse<AdminAchievementListItem> listPaged(String status, String category, String teamName, int page, int pageSize) {
        logger.info("查询成果列表 - 状态:{}, 类型:{}, 团队:{}, 页码:{}, 每页:{}", status, category, teamName, page, pageSize);

        int offset = (page - 1) * pageSize;
        List<AdminAchievementListItem> items = adminAchievementDao.listPaged(status, category, teamName, offset, pageSize);
        int total = adminAchievementDao.countByCondition(status, category, teamName);

        logger.info("查询成果列表完成 - 当前页记录数:{}, 总记录数:{}", items.size(), total);
        return new PagedResponse<>(items, total, page, pageSize);
    }

    /**
     * 获取成果详情
     */
    public AdminAchievementListItem getDetail(Integer id) {
        logger.info("查询成果详情 - ID:{}", id);
        AdminAchievementListItem detail = adminAchievementDao.getDetailById(id);
        if (detail == null) {
            logger.warn("成果不存在 - ID:{}", id);
            return null;
        }
        // 替换proofUrl为签名下载链接（3天有效），兼容带路径的key
        if (detail.getProofUrl() != null && !detail.getProofUrl().isEmpty()) {
            String signedUrl = baiduService.generateBosSignedUrlFromUrl(detail.getProofUrl(), 259200);
            if (signedUrl != null) {
                detail.setProofUrl(signedUrl);
            }
        }
        return detail;
    }

    /**
     * 后端代理下载附件，避免前端签名访问受限
     */
    public boolean writeProofToResponse(Integer id, HttpServletResponse response) {
        AdminAchievementListItem detail = adminAchievementDao.getDetailById(id);
        if (detail == null || detail.getProofUrl() == null || detail.getProofUrl().isEmpty()) {
            return false;
        }
        String key = baiduService.extractKeyFromUrl(detail.getProofUrl());
        if (key == null || key.isEmpty()) {
            return false;
        }
        try {
            BosObject obj = baiduService.getObject(key);
            if (obj == null || obj.getObjectContent() == null) {
                return false;
            }
            String filename = key;
            int idx = key.lastIndexOf('/');
            if (idx != -1 && idx + 1 < key.length()) {
                filename = key.substring(idx + 1);
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
            try (InputStream in = obj.getObjectContent();
                 java.io.OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            return true;
        } catch (Exception e) {
            logger.error("下载附件失败 - ID:{}, key:{}", id, key, e);
            return false;
        }
    }

    /**
     * 审核通过并自动发币
     * @param achievementId 成果ID
     * @param rewardCoins 奖励币值
     * @param reviewedBy 审核人
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAchievement(Integer achievementId, Integer rewardCoins, String reviewedBy) {
        logger.info("开始审核通过成果 - ID:{}, 奖励币值:{}, 审核人:{}", achievementId, rewardCoins, reviewedBy);

        // 1. 检查成果是否存在
        AchievementSubmission achievement = achievementDao.findById(achievementId);
        if (achievement == null) {
            logger.error("审核失败：成果不存在 - ID:{}", achievementId);
            return false;
        }

        // 2. 检查成果是否已审核
        if (!"PENDING".equals(achievement.getStatus())) {
            logger.error("审核失败：成果已审核 - ID:{}, 当前状态:{}", achievementId, achievement.getStatus());
            return false;
        }

        // 3. 更新成果状态为"已通过"
        int updated = achievementDao.approve(achievementId, rewardCoins, reviewedBy);
        if (updated == 0) {
            logger.error("审核失败：更新成果状态失败 - ID:{}", achievementId);
            return false;
        }

        // 4. 给团队发币
        int balanceUpdated = teamDao.addBalance(achievement.getTeamId(), rewardCoins);
        if (balanceUpdated == 0) {
            logger.error("审核失败：更新团队余额失败 - 团队ID:{}, 奖励币值:{}", achievement.getTeamId(), rewardCoins);
            throw new RuntimeException("更新团队余额失败");
        }

        // 5. 记录流水
        Transaction transaction = new Transaction();
        transaction.setTeamId(achievement.getTeamId());
        transaction.setTxnType("ACHIEVEMENT_REWARD");
        transaction.setAmount(rewardCoins);
        transaction.setDescription("成果奖励：" + achievement.getTitle());
        int txnCreated = transactionDao.create(transaction);
        if (txnCreated == 0) {
            logger.error("审核失败：创建流水记录失败 - 团队ID:{}", achievement.getTeamId());
            throw new RuntimeException("创建流水记录失败");
        }

        // 6. 记录管理员操作日志
        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setOperationType("APPROVE_ACHIEVEMENT");
        operationLog.setTargetType("ACHIEVEMENT");
        operationLog.setTargetId(achievementId);
        operationLog.setOperationDetail("审核通过成果：" + achievement.getTitle() + "，奖励币值：" + rewardCoins + "，审核人：" + reviewedBy);
        adminOperationLogDao.insert(operationLog);

        logger.info("成果审核通过并发币成功 - ID:{}, 团队ID:{}, 奖励币值:{}", achievementId, achievement.getTeamId(), rewardCoins);
        return true;
    }

    /**
     * 审核驳回
     * @param achievementId 成果ID
     * @param rejectReason 驳回原因
     * @param reviewedBy 审核人
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectAchievement(Integer achievementId, String rejectReason, String reviewedBy) {
        logger.info("开始审核驳回成果 - ID:{}, 驳回原因:{}, 审核人:{}", achievementId, rejectReason, reviewedBy);

        // 1. 检查成果是否存在
        AchievementSubmission achievement = achievementDao.findById(achievementId);
        if (achievement == null) {
            logger.error("审核失败：成果不存在 - ID:{}", achievementId);
            return false;
        }

        // 2. 检查成果是否已审核
        if (!"PENDING".equals(achievement.getStatus())) {
            logger.error("审核失败：成果已审核 - ID:{}, 当前状态:{}", achievementId, achievement.getStatus());
            return false;
        }

        // 3. 更新成果状态为"已驳回"
        int updated = achievementDao.reject(achievementId, rejectReason, reviewedBy);
        if (updated == 0) {
            logger.error("审核失败：更新成果状态失败 - ID:{}", achievementId);
            return false;
        }

        // 4. 记录管理员操作日志
        AdminOperationLog operationLog = new AdminOperationLog();
        operationLog.setOperationType("REJECT_ACHIEVEMENT");
        operationLog.setTargetType("ACHIEVEMENT");
        operationLog.setTargetId(achievementId);
        operationLog.setOperationDetail("驳回成果：" + achievement.getTitle() + "，原因：" + rejectReason + "，审核人：" + reviewedBy);
        adminOperationLogDao.insert(operationLog);

        logger.info("成果审核驳回成功 - ID:{}", achievementId);
        return true;
    }
}
