package com.campuscoin.service.admin;

import com.campuscoin.dao.admin.AdminTeamDao;
import com.campuscoin.dao.admin.AdminOperationLogDao;
import com.campuscoin.dao.TransactionDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.admin.AdminTeamListItem;
import com.campuscoin.model.admin.AdminTeamDetail;
import com.campuscoin.model.admin.AdminOperationLog;
import com.campuscoin.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 团队管理服务
 * 为后台管理系统提供团队查询、统计、管理操作功能
 */
@Service
public class AdminTeamService {

    private static final Logger logger = LoggerFactory.getLogger(AdminTeamService.class);

    @Autowired
    private AdminTeamDao adminTeamDao;

    @Autowired
    private TeamDao teamDao;

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private AdminOperationLogDao adminOperationLogDao;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页查询团队列表
     */
    public Map<String, Object> listPaged(String keyword, String orderBy, String orderDir, Integer page, Integer pageSize) {
        page = (page == null || page < 1) ? 1 : page;
        pageSize = (pageSize == null || pageSize < 1) ? 20 : pageSize;
        orderBy = (orderBy == null || orderBy.isEmpty()) ? "createdAt" : orderBy;
        orderDir = (orderDir == null || orderDir.isEmpty()) ? "DESC" : orderDir;

        int offset = (page - 1) * pageSize;

        logger.info("查询团队列表 - keyword={}, orderBy={}, orderDir={}, page={}, pageSize={}",
                keyword, orderBy, orderDir, page, pageSize);

        List<AdminTeamListItem> list = adminTeamDao.listPaged(keyword, orderBy, orderDir, offset, pageSize);
        int total = adminTeamDao.countByCondition(keyword);

        logger.info("查询团队列表完成 - 返回 {} 条记录，共 {} 条", list.size(), total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取团队详情（含能力画像）
     */
    public AdminTeamDetail getTeamDetail(Integer teamId) {
        logger.info("查询团队详情 - teamId={}", teamId);

        // 基础信息
        AdminTeamDetail detail = adminTeamDao.getTeamBasicInfo(teamId);
        if (detail == null) {
            logger.warn("团队不存在 - teamId={}", teamId);
            return null;
        }

        // 统计数据
        detail.setTotalAchievements(adminTeamDao.countTotalAchievements(teamId) != null ? adminTeamDao.countTotalAchievements(teamId) : 0);
        detail.setTotalCheckins(adminTeamDao.countTotalCheckins(teamId) != null ? adminTeamDao.countTotalCheckins(teamId) : 0);
        detail.setTotalDutyTasks(adminTeamDao.countTotalDutyTasks(teamId) != null ? adminTeamDao.countTotalDutyTasks(teamId) : 0);
        detail.setTotalTrainings(adminTeamDao.countTotalTrainings(teamId) != null ? adminTeamDao.countTotalTrainings(teamId) : 0);
        detail.setWeeklyResourceUsage(adminTeamDao.countWeeklyResourceUsage(teamId) != null ? adminTeamDao.countWeeklyResourceUsage(teamId) : 0);

        // 能力雷达图分数计算
        // 1. 创新力 = 成果总奖励值
        Integer innovationScore = adminTeamDao.calcInnovationScore(teamId);
        detail.setInnovationScore(innovationScore != null ? innovationScore : 0);

        // 2. 活跃度 = 打卡次数（归一化到0-100，假设100次打卡满分）
        int activityScore = Math.min(100, detail.getTotalCheckins() != null ? detail.getTotalCheckins() : 0);
        detail.setActivityScore(activityScore);

        // 3. 资源利用率 = 本周资源使用次数（归一化到0-100，假设10次使用满分）
        int resourceScore = Math.min(100, (detail.getWeeklyResourceUsage() != null ? detail.getWeeklyResourceUsage() : 0) * 10);
        detail.setResourceScore(resourceScore);

        // 4. 参与度 = 值班+培训次数（归一化到0-100，假设20次参与满分）
        int participationScore = Math.min(100, ((detail.getTotalDutyTasks() != null ? detail.getTotalDutyTasks() : 0) +
                (detail.getTotalTrainings() != null ? detail.getTotalTrainings() : 0)) * 5);
        detail.setParticipationScore(participationScore);

        logger.info("团队详情查询完成 - teamId={}, teamName={}, balance={}, innovationScore={}, activityScore={}, resourceScore={}, participationScore={}",
                teamId, detail.getTeamName(), detail.getBalance(), innovationScore, activityScore, resourceScore, participationScore);

        return detail;
    }

    /**
     * 获取团队近期行为时间线数据
     */
    public Map<String, Object> getTeamTimeline(Integer teamId) {
        logger.info("查询团队行为时间线 - teamId={}", teamId);

        Map<String, Object> timeline = new HashMap<>();
        timeline.put("transactions", adminTeamDao.getRecentTransactions(teamId));
        timeline.put("achievements", adminTeamDao.getRecentAchievements(teamId));
        timeline.put("deviceBookings", adminTeamDao.getRecentDeviceBookings(teamId));
        timeline.put("venueBookings", adminTeamDao.getRecentVenueBookings(teamId));

        logger.info("团队行为时间线查询完成 - teamId={}", teamId);
        return timeline;
    }

    /**
     * 重置团队密码（管理员操作）
     */
    @Transactional
    public boolean resetPassword(Integer teamId, String newPassword, Integer adminId, String ipAddress, String reason) {
        logger.info("重置团队密码 - teamId={}, adminId={}, reason={}", teamId, adminId, reason);

        String passwordHash = passwordEncoder.encode(newPassword);
        // 更新密码（需要在 TeamDao 中添加相应方法）
        int rows = teamDao.updatePasswordHash(teamId, passwordHash);

        if (rows > 0) {
            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setOperationType("RESET_PASSWORD");
            log.setTargetType("TEAM");
            log.setTargetId(teamId);
            log.setOperationDetail("{\"action\":\"resetPassword\"}");
            log.setReason(reason);
            log.setIpAddress(ipAddress);
            adminOperationLogDao.insert(log);

            logger.info("重置团队密码成功 - teamId={}", teamId);
            return true;
        }

        logger.warn("重置团队密码失败 - teamId={}", teamId);
        return false;
    }

    /**
     * 手动调整团队虚拟币（补偿/惩罚）
     */
    @Transactional
    public boolean adjustBalance(Integer teamId, Integer amount, String reason, Integer adminId, String ipAddress) {
        logger.info("手动调整虚拟币 - teamId={}, amount={}, reason={}, adminId={}", teamId, amount, reason, adminId);

        int rows;
        if (amount > 0) {
            rows = teamDao.addBalance(teamId, amount);
        } else {
            rows = teamDao.addBalance(teamId, amount); // 负数相当于扣减
        }

        if (rows > 0) {
            // 记录流水
            Transaction txn = new Transaction();
            txn.setTeamId(teamId);
            txn.setTxnType("ADMIN_ADJUST");
            txn.setAmount(amount);
            txn.setDescription("管理员调整：" + (reason != null ? reason : "无备注"));
            transactionDao.create(txn);

            // 记录操作日志
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminId(adminId);
            log.setOperationType("ADJUST_BALANCE");
            log.setTargetType("TEAM");
            log.setTargetId(teamId);
            log.setOperationDetail("{\"amount\":" + amount + ",\"txnId\":" + txn.getId() + "}");
            log.setReason(reason);
            log.setIpAddress(ipAddress);
            adminOperationLogDao.insert(log);

            logger.info("手动调整虚拟币成功 - teamId={}, amount={}, txnId={}", teamId, amount, txn.getId());
            return true;
        }

        logger.warn("手动调整虚拟币失败 - teamId={}", teamId);
        return false;
    }
}
