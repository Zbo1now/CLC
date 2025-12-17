package com.campuscoin.service.admin;

import com.campuscoin.dao.admin.AdminActivityDao;
import com.campuscoin.dao.admin.AdminOperationLogDao;
import com.campuscoin.model.Activity;
import com.campuscoin.model.admin.AdminActivityListItem;
import com.campuscoin.model.admin.AdminOperationLog;
import com.campuscoin.payload.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

/**
 * 活动管理服务
 */
@Service
public class AdminActivityService {

    private static final Logger logger = LoggerFactory.getLogger(AdminActivityService.class);

    @Autowired
    private AdminActivityDao adminActivityDao;

    @Autowired
    private AdminOperationLogDao adminOperationLogDao;

    /**
     * 分页查询活动列表
     */
    public PagedResponse<AdminActivityListItem> listPaged(
            String activityType,
            String status,
            String searchKeyword,
            Integer page,
            Integer pageSize
    ) {
        logger.info("分页查询活动列表: type={}, status={}, keyword={}, page={}, pageSize={}", 
                    activityType, status, searchKeyword, page, pageSize);

        int offset = (page - 1) * pageSize;

        // 查询活动列表
        List<AdminActivityListItem> items = adminActivityDao.listPaged(
                activityType, status, searchKeyword, offset, pageSize
        );

        // 查询总数
        int total = adminActivityDao.countByCondition(activityType, status, searchKeyword);

        logger.info("查询到 {} 条活动记录，当前页 {} 条", total, items.size());

        PagedResponse<AdminActivityListItem> response = new PagedResponse<>();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setPageSize(pageSize);

        return response;
    }

    /**
     * 获取活动详情
     */
    public Activity getDetail(Integer id) {
        logger.info("获取活动详情: id={}", id);

        Activity activity = adminActivityDao.findById(id);
        if (activity == null) {
            logger.warn("活动不存在: id={}", id);
        }

        return activity;
    }

    /**
     * 创建活动
     */
    @Transactional(rollbackFor = Exception.class)
    public Activity create(Activity activity, String adminUsername) {
        logger.info("创建活动: activityName={}, activityType={}, createdBy={}", 
                    activity.getActivityName(), activity.getActivityType(), adminUsername);

        // 设置创建信息
        activity.setCreatedBy(adminUsername);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);
        activity.setCurrentParticipants(0);

        // 根据开始时间自动设置状态
        Timestamp startTime = activity.getStartTime();
        Timestamp endTime = activity.getEndTime();

        if (now.before(startTime)) {
            activity.setStatus("NOT_STARTED");
        } else if (now.after(endTime)) {
            activity.setStatus("FINISHED");
        } else {
            activity.setStatus("ONGOING");
        }

        // 创建活动
        adminActivityDao.create(activity);

        logger.info("活动创建成功: id={}, activityName={}", activity.getId(), activity.getActivityName());

        // 记录操作日志
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(null); // 使用 null 因为 adminId 是 Integer 类型
        log.setOperationType("CREATE_ACTIVITY");
        log.setTargetType("ACTIVITY");
        log.setTargetId(activity.getId());
        log.setOperationDetail("创建活动: " + activity.getActivityName());
        adminOperationLogDao.insert(log);

        return activity;
    }

    /**
     * 更新活动
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean update(Integer id, Activity activity, String adminUsername) {
        logger.info("更新活动: id={}, adminUsername={}", id, adminUsername);

        // 检查活动是否存在
        Activity existingActivity = adminActivityDao.findById(id);
        if (existingActivity == null) {
            logger.warn("活动不存在: id={}", id);
            return false;
        }

        // 只能编辑未开始的活动
        if (!"NOT_STARTED".equals(existingActivity.getStatus())) {
            logger.warn("活动已开始或已结束，无法编辑: id={}, status={}", id, existingActivity.getStatus());
            return false;
        }

        // 更新字段
        activity.setId(id);
        activity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // 更新活动
        int updated = adminActivityDao.update(activity);

        if (updated > 0) {
            logger.info("活动更新成功: id={}, activityName={}", id, activity.getActivityName());

            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(null);
            log.setOperationType("UPDATE_ACTIVITY");
            log.setTargetType("ACTIVITY");
            log.setTargetId(id);
            log.setOperationDetail("更新活动: " + activity.getActivityName());
            adminOperationLogDao.insert(log);
        } else {
            logger.error("活动更新失败: id={}", id);
        }

        return updated > 0;
    }

    /**
     * 更新活动状态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Integer id, String status, String adminUsername) {
        logger.info("更新活动状态: id={}, status={}, adminUsername={}", id, status, adminUsername);

        // 检查活动是否存在
        Activity activity = adminActivityDao.findById(id);
        if (activity == null) {
            logger.warn("活动不存在: id={}", id);
            return false;
        }

        // 更新状态
        int updated = adminActivityDao.updateStatus(id, status);

        if (updated > 0) {
            logger.info("活动状态更新成功: id={}, oldStatus={}, newStatus={}", 
                       id, activity.getStatus(), status);

            // 记录操作日志
            String actionName = "CANCELLED".equals(status) ? "下线活动" : "更新活动状态";
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(null);
            log.setOperationType("UPDATE_ACTIVITY_STATUS");
            log.setTargetType("ACTIVITY");
            log.setTargetId(id);
            log.setOperationDetail(actionName + ": " + activity.getActivityName() + " -> " + status);
            adminOperationLogDao.insert(log);
        } else {
            logger.error("活动状态更新失败: id={}", id);
        }

        return updated > 0;
    }

    /**
     * 删除活动（仅未开始的活动）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Integer id, String adminUsername) {
        logger.info("删除活动: id={}, adminUsername={}", id, adminUsername);

        // 检查活动是否存在
        Activity activity = adminActivityDao.findById(id);
        if (activity == null) {
            logger.warn("活动不存在: id={}", id);
            return false;
        }

        // 只能删除未开始的活动
        if (!"NOT_STARTED".equals(activity.getStatus())) {
            logger.warn("活动已开始或已结束，无法删除: id={}, status={}", id, activity.getStatus());
            return false;
        }

        // 删除活动（参与记录会被级联删除）
        int deleted = adminActivityDao.deleteById(id);

        if (deleted > 0) {
            logger.info("活动删除成功: id={}, activityName={}", id, activity.getActivityName());

            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(null);
            log.setOperationType("DELETE_ACTIVITY");
            log.setTargetType("ACTIVITY");
            log.setTargetId(id);
            log.setOperationDetail("删除活动: " + activity.getActivityName());
            adminOperationLogDao.insert(log);
        } else {
            logger.error("活动删除失败: id={}", id);
        }

        return deleted > 0;
    }

    /**
     * 自动更新活动状态（定时任务调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public int autoUpdateStatus() {
        logger.info("开始自动更新活动状态");

        int updated = adminActivityDao.autoUpdateStatus();

        logger.info("自动更新活动状态完成，共更新 {} 条记录", updated);

        return updated;
    }
}
