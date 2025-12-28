package com.campuscoin.service.admin;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.TransactionDao;
import com.campuscoin.dao.admin.AdminActivityDao;
import com.campuscoin.dao.admin.AdminOperationLogDao;
import com.campuscoin.dao.admin.AdminParticipationDao;
import com.campuscoin.model.Activity;
import com.campuscoin.model.ActivityParticipation;
import com.campuscoin.model.Transaction;
import com.campuscoin.model.admin.AdminOperationLog;
import com.campuscoin.model.admin.AdminParticipationListItem;
import com.campuscoin.payload.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 活动参与记录管理服务
 */
@Service
public class AdminParticipationService {
    @Autowired
    private com.campuscoin.service.BaiduService baiduService;

    private static final Logger logger = LoggerFactory.getLogger(AdminParticipationService.class);

    @Autowired
    private AdminParticipationDao adminParticipationDao;

    @Autowired
    private AdminActivityDao adminActivityDao;

    @Autowired
    private TeamDao teamDao;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private AdminOperationLogDao adminOperationLogDao;

    /**
     * 分页查询参与记录列表
     */
    public PagedResponse<AdminParticipationListItem> listPaged(
            Integer activityId,
            String reviewStatus,
            String searchKeyword,
            Integer page,
            Integer pageSize
    ) {
        logger.info("分页查询参与记录列表: activityId={}, reviewStatus={}, keyword={}, page={}, pageSize={}",
                    activityId, reviewStatus, searchKeyword, page, pageSize);

        int offset = (page - 1) * pageSize;

        // 查询参与记录列表
        List<AdminParticipationListItem> items = adminParticipationDao.listPaged(
                activityId, reviewStatus, searchKeyword, pageSize, offset
        );

        // 查询总数
        int total = adminParticipationDao.countByCondition(activityId, reviewStatus, searchKeyword);

        logger.info("查询到 {} 条参与记录，当前页 {} 条", total, items.size());

        PagedResponse<AdminParticipationListItem> response = new PagedResponse<>();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        // 自动补充标准化 proofUrl 字段，便于前端直接展示
        for (AdminParticipationListItem item : items) {
            String proofUrl = item.getProofUrl();
            // 兼容路径型/历史数据，统一转为BOS公有读域名型
            if (proofUrl != null && !proofUrl.isEmpty()) {
                // 这里建议注入BaiduService为成员变量
                item.setProofUrl(baiduService.toBosPublicUrl(proofUrl));
            }
        }

        return response;
    }

    /**
     * 获取参与记录详情
     */
    public ActivityParticipation getDetail(Integer id) {
        logger.info("获取参与记录详情: id={}", id);

        ActivityParticipation participation = adminParticipationDao.findById(id);
        if (participation == null) {
            logger.warn("参与记录不存在: id={}", id);
        }

        return participation;
    }

    /**
     * 审核通过（自动发币）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Integer id, String adminUsername) {
        logger.info("审核通过: id={}, adminUsername={}", id, adminUsername);

        // 查询参与记录
        ActivityParticipation participation = adminParticipationDao.findById(id);
        if (participation == null) {
            logger.warn("参与记录不存在: id={}", id);
            return false;
        }

        // 检查是否已审核
        if (!"PENDING".equals(participation.getReviewStatus())) {
            logger.warn("参与记录已审核: id={}, reviewStatus={}", id, participation.getReviewStatus());
            return false;
        }

        // 查询活动信息
        Activity activity = adminActivityDao.findById(participation.getActivityId());
        if (activity == null) {
            logger.warn("活动不存在: activityId={}", participation.getActivityId());
            return false;
        }

        // 检查活动是否已取消
        if ("CANCELLED".equals(activity.getStatus())) {
            logger.warn("活动已取消，无法审核通过: activityId={}", activity.getId());
            return false;
        }

        // 确保inUsername不为null
        String reviewer = (adminUsername != null && !adminUsername.isEmpty()) ? adminUsername : "admin";

        // 1. 更新参与记录状态（包括发币金额）
        int updated = adminParticipationDao.approve(id, reviewer, activity.getRewardCoins());
        if (updated == 0) {
            logger.error("更新参与记录失败: id={}", id);
            return false;
        }

        // 2. 增加活动参与人数
        adminActivityDao.incrementParticipants(activity.getId());

        // 3. 给团队增加虚拟币
        if (activity.getRewardCoins() > 0) {
            teamDao.addBalance(participation.getTeamId(), activity.getRewardCoins());
            logger.info("给团队增加虚拟币: teamId={}, coins={}", participation.getTeamId(), activity.getRewardCoins());

            // 4. 创建流水记录（根据活动类型使用不同的交易类型）
            Transaction txn = new Transaction();
            txn.setTeamId(participation.getTeamId());
            String txnType = "DUTY".equals(activity.getActivityType()) ? "DUTY_REWARD" : "TRAINING_REWARD";
            txn.setTxnType(txnType);
            txn.setAmount(activity.getRewardCoins());
            txn.setDescription("活动奖励: " + activity.getActivityName());
            transactionDao.create(txn);
        }

        logger.info("审核通过成功: id={}, activityName={}, teamName={}, coins={}",
                   id, activity.getActivityName(), participation.getTeamName(), activity.getRewardCoins());

        // 5. 记录操作日志
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(null);
        log.setOperationType("APPROVE_PARTICIPATION");
        log.setTargetType("ACTIVITY_PARTICIPATION");
        log.setTargetId(id);
        log.setOperationDetail("审核通过: " + activity.getActivityName() + " - " + participation.getTeamName());
        adminOperationLogDao.insert(log);

        return true;
    }

    /**
     * 审核驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Integer id, String rejectReason, String adminUsername) {
        logger.info("审核驳回: id={}, rejectReason={}, adminUsername={}", id, rejectReason, adminUsername);

        // 查询参与记录
        ActivityParticipation participation = adminParticipationDao.findById(id);
        if (participation == null) {
            logger.warn("参与记录不存在: id={}", id);
            return false;
        }

        // 检查是否已审核
        if (!"PENDING".equals(participation.getReviewStatus())) {
            logger.warn("参与记录已审核: id={}, reviewStatus={}", id, participation.getReviewStatus());
            return false;
        }

        // 查询活动信息
        Activity activity = adminActivityDao.findById(participation.getActivityId());
        if (activity == null) {
            logger.warn("活动不存在: activityId={}", participation.getActivityId());
            return false;
        }

        // 更新参与记录状态
        int updated = adminParticipationDao.reject(id, rejectReason, adminUsername);

        if (updated > 0) {
            logger.info("审核驳回成功: id={}, activityName={}, teamName={}",
                       id, activity.getActivityName(), participation.getTeamName());

            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(null);
            log.setOperationType("REJECT_PARTICIPATION");
            log.setTargetType("ACTIVITY_PARTICIPATION");
            log.setTargetId(id);
            log.setOperationDetail("审核驳回: " + activity.getActivityName() + " - " + participation.getTeamName());
            adminOperationLogDao.insert(log);
        } else {
            logger.error("审核驳回失败: id={}", id);
        }

        return updated > 0;
    }

    /**
     * 批量审核通过
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Integer> ids, String adminUsername) {
        logger.info("批量审核通过: ids={}, count={}, adminUsername={}", ids, ids.size(), adminUsername);

        if (ids == null || ids.isEmpty()) {
            logger.warn("批量审核参数为空");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        // 逐个审核通过（保证事务一致性）
        for (Integer id : ids) {
            try {
                boolean success = approve(id, adminUsername);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("批量审核失败: id={}, error={}", id, e.getMessage(), e);
                failCount++;
            }
        }

        logger.info("批量审核完成: 成功 {} 条，失败 {} 条", successCount, failCount);

        // 记录操作日志
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(null);
        log.setOperationType("BATCH_APPROVE_PARTICIPATION");
        log.setTargetType("ACTIVITY_PARTICIPATION");
        log.setOperationDetail("批量审核通过: 成功 " + successCount + " 条，失败 " + failCount + " 条");
        adminOperationLogDao.insert(log);

        return successCount;
    }

    /**
     * 标记已完成并发放奖励
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markCompleted(Integer id, String completionNotes, String adminUsername) {
        logger.info("标记已完成: id={}, completionNotes={}, adminUsername={}", id, completionNotes, adminUsername);

        // 查询参与记录
        ActivityParticipation participation = adminParticipationDao.findById(id);
        if (participation == null) {
            logger.warn("参与记录不存在: id={}", id);
            return false;
        }

        // 检查是否已审核通过
        if (!"APPROVED".equals(participation.getReviewStatus())) {
            logger.warn("参与记录未审核通过: id={}, reviewStatus={}", id, participation.getReviewStatus());
            return false;
        }

        // 检查是否已完成
        if ("COMPLETED".equals(participation.getStatus())) {
            logger.warn("参与记录已完成: id={}", id);
            return false;
        }

        // 查询活动信息
        Activity activity = adminActivityDao.findById(participation.getActivityId());
        if (activity == null) {
            logger.warn("活动不存在: activityId={}", participation.getActivityId());
            return false;
        }

        // 更新参与记录状态
        int updated = adminParticipationDao.markCompleted(id, activity.getRewardCoins());

        if (updated > 0) {
            logger.info("标记已完成成功: id={}, activityName={}, teamName={}",
                       id, activity.getActivityName(), participation.getTeamName());

            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(null);
            log.setOperationType("MARK_PARTICIPATION_COMPLETED");
            log.setTargetType("ACTIVITY_PARTICIPATION");
            log.setTargetId(id);
            log.setOperationDetail("标记已完成: " + activity.getActivityName() + " - " + participation.getTeamName());
            adminOperationLogDao.insert(log);
        } else {
            logger.error("标记已完成失败: id={}", id);
        }

        return updated > 0;
    }

    /**
     * 批量标记已完成
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchMarkCompleted(List<Integer> ids, String completionNotes, String adminUsername) {
        logger.info("批量标记已完成: ids={}, count={}, adminUsername={}", ids, ids.size(), adminUsername);

        if (ids == null || ids.isEmpty()) {
            logger.warn("批量标记参数为空");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        // 逐个标记已完成（保证事务一致性）
        for (Integer id : ids) {
            try {
                boolean success = markCompleted(id, completionNotes, adminUsername);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("批量标记失败: id={}, error={}", id, e.getMessage(), e);
                failCount++;
            }
        }

        logger.info("批量标记完成: 成功 {} 条，失败 {} 条", successCount, failCount);

        // 记录操作日志
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(null);
        log.setOperationType("BATCH_MARK_COMPLETED");
        log.setTargetType("ACTIVITY_PARTICIPATION");
        log.setOperationDetail("批量标记已完成: 成功 " + successCount + " 条，失败 " + failCount + " 条");
        adminOperationLogDao.insert(log);

        return successCount;
    }
}
