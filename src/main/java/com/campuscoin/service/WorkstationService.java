package com.campuscoin.service;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.WorkstationDao;
import com.campuscoin.dao.WorkstationLeaseDao;
import com.campuscoin.model.Workstation;
import com.campuscoin.model.WorkstationLease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class WorkstationService {

    private static final Logger logger = LoggerFactory.getLogger(WorkstationService.class);

    private final WorkstationDao workstationDao;
    private final WorkstationLeaseDao leaseDao;
    private final TeamDao teamDao;

    public WorkstationService(WorkstationDao workstationDao, WorkstationLeaseDao leaseDao, TeamDao teamDao) {
        this.workstationDao = workstationDao;
        this.leaseDao = leaseDao;
        this.teamDao = teamDao;
    }

    public List<Workstation> listWorkstationsForCurrentMonth() {
        Date currentMonthStart = Date.valueOf(LocalDate.now().withDayOfMonth(1));
        return workstationDao.listAllWithCurrentMonthStatus(currentMonthStart);
    }

    public WorkstationLease getMyActiveLease(int teamId) {
        // “我的租约”页需要展示最近一条租约，便于到期后续租/释放
        return leaseDao.findLatestActiveByTeam(teamId);
    }

    @Transactional
    public WorkstationLease rent(int teamId, int workstationId, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("租赁月数必须大于0");
        }
        if (months > 12) {
            throw new IllegalArgumentException("单次最多租赁12个月");
        }

        // 并发保护：锁住工位行，避免同一工位被并发租用
        Workstation workstation = workstationDao.findByIdForUpdate(workstationId);
        if (workstation == null) {
            throw new IllegalArgumentException("工位不存在");
        }

        LocalDate now = LocalDate.now();
        Date startMonth = Date.valueOf(now.withDayOfMonth(1));
        Date endMonth = Date.valueOf(now.withDayOfMonth(1).plusMonths(months - 1));

        int overlap = leaseDao.countOverlappingActive(workstationId, startMonth, endMonth);
        if (overlap > 0) {
            throw new IllegalStateException("该工位在所选租期内已被租用");
        }

        int monthlyRent = workstation.getMonthlyRent();
        int totalCost = monthlyRent * months;

        // 余额扣费（原子校验）：余额不足则更新行数为0
        int deducted = teamDao.deductBalanceIfEnough(teamId, totalCost);
        if (deducted <= 0) {
            throw new IllegalStateException("余额不足，无法租用");
        }

        WorkstationLease lease = new WorkstationLease();
        lease.setTeamId(teamId);
        lease.setWorkstationId(workstationId);
        lease.setStartMonth(startMonth);
        lease.setEndMonth(endMonth);
        lease.setMonthlyRent(monthlyRent);
        lease.setTotalCost(totalCost);
        lease.setStatus("ACTIVE");

        leaseDao.create(lease);

        logger.info("工位租赁成功: teamId={}, workstationId={}, months={}, cost={}, period={}~{}",
                teamId, workstationId, months, totalCost, startMonth, endMonth);

        return lease;
    }

    @Transactional
    public WorkstationLease renewMyLease(int teamId, int months) {
        if (months <= 0) {
            throw new IllegalArgumentException("续租月数必须大于0");
        }
        if (months > 12) {
            throw new IllegalArgumentException("单次最多续租12个月");
        }

        LocalDate currentMonthStartLd = LocalDate.now().withDayOfMonth(1);
        Date currentMonthStart = Date.valueOf(currentMonthStartLd);

        // 允许“到期后续租”：使用最近一条 ACTIVE 租约作为续租基准
        WorkstationLease latest = leaseDao.findLatestActiveByTeam(teamId);
        if (latest == null) {
            throw new IllegalStateException("暂无可续租的租约");
        }

        // 锁住租约行，避免并发续租重复扣费
        WorkstationLease locked = leaseDao.findByIdForUpdate(latest.getId());
        if (locked == null || locked.getTeamId() != teamId) {
            throw new IllegalStateException("租约状态异常");
        }

        LocalDate leaseEnd = locked.getEndMonth().toLocalDate();
        LocalDate expectedNextMonth = leaseEnd.plusMonths(1);
        // 如果已过期很久（产生空档月份），要求重新租用，避免用“续租”跨越空档
        if (expectedNextMonth.isBefore(currentMonthStartLd)) {
            throw new IllegalStateException("租约已过期，请重新租用");
        }

        // 续租起始月：正常为 end_month + 1；如果当前月就是到期后的下个月，也会等于 currentMonthStart
        LocalDate renewStart = expectedNextMonth;
        Date newStart = Date.valueOf(renewStart);
        Date newEnd = Date.valueOf(renewStart.plusMonths(months - 1));

        // 并发保护：锁住工位行 + 校验续租区间是否被其他租约占用
        workstationDao.findByIdForUpdate(locked.getWorkstationId());
        int overlap = leaseDao.countOverlappingActiveExclude(locked.getWorkstationId(), locked.getId(), newStart, newEnd);
        if (overlap > 0) {
            throw new IllegalStateException("续租失败：该工位后续月份已被占用");
        }

        int totalCost = locked.getMonthlyRent() * months;
        int deducted = teamDao.deductBalanceIfEnough(teamId, totalCost);
        if (deducted <= 0) {
            throw new IllegalStateException("余额不足，无法续租");
        }

        Date finalEnd = newEnd;
        int newTotalCost = locked.getTotalCost() + totalCost;
        leaseDao.updateEndMonthAndCost(locked.getId(), finalEnd, newTotalCost);

        logger.info("工位续租成功: teamId={}, leaseId={}, workstationId={}, months={}, cost={}, newEnd={}",
                teamId, locked.getId(), locked.getWorkstationId(), months, totalCost, finalEnd);

        // 返回更新后的租约（简单起见，直接修改内存对象）
        locked.setEndMonth(finalEnd);
        locked.setTotalCost(newTotalCost);
        return locked;
    }

    @Transactional
    public void releaseMyLeaseIfExpired(int teamId) {
        LocalDate currentMonthStartLd = LocalDate.now().withDayOfMonth(1);
        Date currentMonthStart = Date.valueOf(currentMonthStartLd);

        WorkstationLease latest = leaseDao.findLatestActiveByTeam(teamId);
        if (latest == null) {
            throw new IllegalStateException("当前无可释放的租约");
        }

        WorkstationLease locked = leaseDao.findByIdForUpdate(latest.getId());
        if (locked == null || locked.getTeamId() != teamId) {
            throw new IllegalStateException("租约状态异常");
        }

        // 仍在有效期内，不允许释放（按自然月计费）
        if (!locked.getEndMonth().toLocalDate().isBefore(currentMonthStartLd)) {
            throw new IllegalStateException("租期未到期，暂不支持提前释放");
        }

        int updated = leaseDao.release(locked.getId());
        if (updated <= 0) {
            throw new IllegalStateException("释放失败，请稍后重试");
        }

        logger.info("工位租约释放成功: teamId={}, leaseId={}, workstationId={}, endMonth={}",
                teamId, locked.getId(), locked.getWorkstationId(), locked.getEndMonth());
    }
}
