package com.campuscoin.service;

import com.campuscoin.dao.DutyTaskDao;
import com.campuscoin.dao.DutyTaskSignupDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.DutyTask;
import com.campuscoin.model.DutyTaskSignup;
import com.campuscoin.model.DutyTaskSignupCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DutyTaskService {

    private static final Logger logger = LoggerFactory.getLogger(DutyTaskService.class);

    private final DutyTaskDao dutyTaskDao;
    private final DutyTaskSignupDao dutyTaskSignupDao;
    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public DutyTaskService(DutyTaskDao dutyTaskDao,
                           DutyTaskSignupDao dutyTaskSignupDao,
                           TeamDao teamDao,
                           TransactionService transactionService) {
        this.dutyTaskDao = dutyTaskDao;
        this.dutyTaskSignupDao = dutyTaskSignupDao;
        this.teamDao = teamDao;
        this.transactionService = transactionService;
    }

    public List<DutyTask> listForTeam(int teamId) {
        List<DutyTask> tasks = dutyTaskDao.listAll();
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> taskIds = new ArrayList<>();
        for (DutyTask t : tasks) {
            if (t != null && t.getId() != null) {
                taskIds.add(t.getId());
            }
        }

        Map<Integer, Integer> counts = new HashMap<>();
        try {
            List<DutyTaskSignupCount> grouped = dutyTaskSignupDao.countActiveGrouped(taskIds);
            if (grouped != null) {
                for (DutyTaskSignupCount row : grouped) {
                    if (row.getTaskId() != null) {
                        counts.put(row.getTaskId(), row.getCount() == null ? 0 : row.getCount());
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("统计报名人数失败，将降级为逐条统计: teamId={}", teamId, e);
        }

        Map<Integer, DutyTaskSignup> mySignups = new HashMap<>();
        try {
            List<DutyTaskSignup> mine = dutyTaskSignupDao.listByTeamAndTaskIds(teamId, taskIds);
            if (mine != null) {
                for (DutyTaskSignup s : mine) {
                    if (s.getTaskId() != null) {
                        mySignups.put(s.getTaskId(), s);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("查询我的报名失败: teamId={}", teamId, e);
        }

        LocalDateTime now = LocalDateTime.now();

        for (DutyTask t : tasks) {
            if (t == null || t.getId() == null) {
                continue;
            }

            LocalDateTime st = t.getStartTime() != null ? t.getStartTime().toLocalDateTime() : null;
            LocalDateTime et = t.getEndTime() != null ? t.getEndTime().toLocalDateTime() : null;

            String taskStatus = computeTaskStatus(now, st, et);
            t.setTaskStatus(taskStatus);

            int signupCount = counts.containsKey(t.getId()) ? counts.get(t.getId()) : safeCountActive(t.getId());
            t.setSignupCount(signupCount);

            int required = t.getRequiredPeople() == null ? 0 : t.getRequiredPeople();
            t.setRemaining(Math.max(0, required - signupCount));

            DutyTaskSignup s = mySignups.get(t.getId());
            String myStatus = null;
            if (s != null && s.getSignupStatus() != null) {
                String raw = s.getSignupStatus().trim().toUpperCase();
                if ("COMPLETED".equals(raw)) {
                    myStatus = "COMPLETED";
                } else {
                    // SIGNED: 若任务已结束，则展示“待确认”
                    if (et != null && !now.isBefore(et)) {
                        myStatus = "PENDING_CONFIRM";
                    } else {
                        myStatus = "SIGNED";
                    }
                }
            }
            t.setMySignupStatus(myStatus);
        }

        return tasks;
    }

    @Transactional
    public DutyTaskSignup signup(int teamId, int taskId) {
        DutyTask task = dutyTaskDao.findByIdForUpdate(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = task.getEndTime() != null ? task.getEndTime().toLocalDateTime() : null;
        if (end != null && !now.isBefore(end)) {
            throw new IllegalStateException("任务已结束，无法报名");
        }

        DutyTaskSignup existing = dutyTaskSignupDao.findByTaskAndTeam(taskId, teamId);
        if (existing != null) {
            throw new IllegalStateException("你已报名该任务");
        }

        int required = task.getRequiredPeople() == null ? 0 : task.getRequiredPeople();
        int cnt = dutyTaskSignupDao.countActiveByTask(taskId);
        if (required > 0 && cnt >= required) {
            throw new IllegalStateException("报名人数已满");
        }

        DutyTaskSignup signup = new DutyTaskSignup();
        signup.setTaskId(taskId);
        signup.setTeamId(teamId);
        signup.setSignupStatus("SIGNED");
        dutyTaskSignupDao.create(signup);

        logger.info("值班任务报名成功: teamId={}, taskId={}, name={}", teamId, taskId, task.getTaskName());
        return dutyTaskSignupDao.findByTaskAndTeam(taskId, teamId);
    }

    @Transactional
    public DutyTaskSignup adminConfirmSignup(int signupId, String adminUser) {
        DutyTaskSignup locked = dutyTaskSignupDao.findByIdForUpdate(signupId);
        if (locked == null) {
            throw new IllegalArgumentException("报名记录不存在");
        }

        if (locked.getSignupStatus() != null && "COMPLETED".equalsIgnoreCase(locked.getSignupStatus())) {
            throw new IllegalStateException("该报名已确认");
        }

        DutyTask task = dutyTaskDao.findByIdForUpdate(locked.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = task.getEndTime() != null ? task.getEndTime().toLocalDateTime() : null;
        if (end != null && now.isBefore(end)) {
            throw new IllegalStateException("任务未结束，无法确认");
        }

        int reward = task.getRewardCoins() == null ? 0 : task.getRewardCoins();

        int rows = dutyTaskSignupDao.confirm(signupId, "COMPLETED", reward);
        if (rows != 1) {
            throw new IllegalStateException("确认失败，请刷新重试");
        }

        // 发币：余额增加 + 写流水
        teamDao.addBalance(locked.getTeamId(), reward);
        try {
            String desc = "值班任务完成奖励 - " + (task.getTaskName() == null ? "值班任务" : task.getTaskName());
            transactionService.record(locked.getTeamId(), "DUTY_REWARD", reward, desc);
        } catch (Exception e) {
            logger.error("值班奖励流水记录失败: teamId={}, signupId={}, reward={}", locked.getTeamId(), signupId, reward, e);
        }

        logger.info("管理员确认值班完成并发币: admin={}, teamId={}, signupId={}, taskId={}, reward={}",
                adminUser, locked.getTeamId(), signupId, locked.getTaskId(), reward);

        return dutyTaskSignupDao.findByTaskAndTeam(locked.getTaskId(), locked.getTeamId());
    }

    private int safeCountActive(int taskId) {
        try {
            return dutyTaskSignupDao.countActiveByTask(taskId);
        } catch (Exception e) {
            return 0;
        }
    }

    private String computeTaskStatus(LocalDateTime now, LocalDateTime start, LocalDateTime end) {
        if (start != null && now.isBefore(start)) {
            return "NOT_STARTED";
        }
        if (end != null && now.isBefore(end)) {
            return "IN_PROGRESS";
        }
        return "ENDED";
    }
}
