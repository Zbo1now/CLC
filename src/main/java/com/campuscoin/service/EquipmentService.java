package com.campuscoin.service;

import com.campuscoin.dao.EquipmentDao;
import com.campuscoin.dao.EquipmentLoanDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.Equipment;
import com.campuscoin.model.EquipmentLoan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EquipmentService {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentDao equipmentDao;
    private final EquipmentLoanDao equipmentLoanDao;
    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public EquipmentService(EquipmentDao equipmentDao,
                            EquipmentLoanDao equipmentLoanDao,
                            TeamDao teamDao,
                            TransactionService transactionService) {
        this.equipmentDao = equipmentDao;
        this.equipmentLoanDao = equipmentLoanDao;
        this.teamDao = teamDao;
        this.transactionService = transactionService;
    }

    public List<Equipment> listEquipments(LocalDate startDate, LocalDate endDate) {
        List<Equipment> list = equipmentDao.listAll();
        boolean hasRange = startDate != null && endDate != null;

        for (Equipment e : list) {
            if (e.getStatus() != null && "MAINTENANCE".equalsIgnoreCase(e.getStatus())) {
                e.setAvailable(false);
                continue;
            }

            if (!hasRange) {
                e.setAvailable(true);
                continue;
            }

            int cnt = equipmentLoanDao.countOverlappingActive(e.getId(), startDate, endDate);
            e.setAvailable(cnt == 0);
        }

        logger.info("查询器材列表: range={}~{}, count={}", startDate, endDate, list != null ? list.size() : 0);
        return list;
    }

    public List<EquipmentLoan> listOccupied(int equipmentId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("缺少日期范围");
        }
        List<EquipmentLoan> list = equipmentLoanDao.listOverlappingActive(equipmentId, startDate, endDate);
        logger.info("查询器材占用: equipmentId={}, range={}~{}, count={}", equipmentId, startDate, endDate,
                list != null ? list.size() : 0);
        return list;
    }

    public Map<String, Object> getMyActive(int teamId) {
        EquipmentLoan loan = equipmentLoanDao.findMyActive(teamId);
        Equipment equipment = null;
        if (loan != null) {
            equipment = equipmentDao.findById(loan.getEquipmentId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("loan", loan);
        data.put("equipment", equipment);
        return data;
    }

    @Transactional
    public Map<String, Object> createLoan(int teamId, int equipmentId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("请选择借用日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        Equipment equipment = equipmentDao.findByIdForUpdate(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("器材不存在");
        }
        if (equipment.getStatus() != null && "MAINTENANCE".equalsIgnoreCase(equipment.getStatus())) {
            throw new IllegalStateException("该器材维护中，暂不可借");
        }

        int overlap = equipmentLoanDao.countOverlappingActive(equipmentId, startDate, endDate);
        if (overlap > 0) {
            throw new IllegalStateException("该时间段已被占用，请选择其他日期");
        }

        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 0) {
            throw new IllegalArgumentException("借用天数不合法");
        }

        int cost = Math.max(0, equipment.getRatePerDay()) * days;
        int affected = teamDao.deductBalanceIfEnough(teamId, cost);
        if (affected <= 0) {
            throw new IllegalStateException("余额不足，无法提交借用申请");
        }

        EquipmentLoan loan = new EquipmentLoan();
        loan.setTeamId(teamId);
        loan.setEquipmentId(equipmentId);
        loan.setStartDate(startDate);
        loan.setEndDate(endDate);
        loan.setStatus("PENDING");
        loan.setHeldCost(cost);
        equipmentLoanDao.create(loan);

        String desc = String.format("器材借用预扣：%s（%s~%s）", equipment.getEquipmentName(), startDate, endDate);
        transactionService.record(teamId, "EQUIPMENT_HOLD", -cost, desc);

        logger.info("提交器材借用申请: teamId={}, equipmentId={}, days={}, cost={}, loanId={}",
                teamId, equipmentId, days, cost, loan.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("loan", loan);
        data.put("days", days);
        data.put("cost", cost);
        return data;
    }

    @Transactional
    public EquipmentLoan adminMarkBorrowed(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        if (!"PENDING".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可借出");
        }

        int updated = equipmentLoanDao.markBorrowed(loanId);
        if (updated <= 0) {
            throw new IllegalStateException("更新失败，请重试");
        }

        EquipmentLoan latest = equipmentLoanDao.findById(loanId);
        logger.info("管理员标记已借出: loanId={}, teamId={}, equipmentId={}", loanId, latest.getTeamId(), latest.getEquipmentId());
        return latest;
    }

    @Transactional
    public EquipmentLoan adminMarkReturned(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        if (!"BORROWED".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可归还");
        }

        int updated = equipmentLoanDao.markReturned(loanId);
        if (updated <= 0) {
            throw new IllegalStateException("更新失败，请重试");
        }

        EquipmentLoan latest = equipmentLoanDao.findById(loanId);
        logger.info("管理员标记已归还: loanId={}, teamId={}, equipmentId={}", loanId, latest.getTeamId(), latest.getEquipmentId());
        return latest;
    }

    public List<EquipmentLoan> adminListByStatus(String status) {
        String st = (status == null || status.trim().isEmpty()) ? "PENDING" : status.trim().toUpperCase();
        List<EquipmentLoan> list = equipmentLoanDao.adminListByStatus(st);
        logger.info("管理员查询器材借用: status={}, count={}", st, list != null ? list.size() : 0);
        return list;
    }
}
