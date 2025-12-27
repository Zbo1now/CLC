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

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
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

            int used = equipmentLoanDao.sumOverlappingActiveQuantity(e.getId(), startDate, endDate);
            int total = Math.max(0, e.getQuantity());
            int remaining = Math.max(0, total - Math.max(0, used));
            e.setAvailable(remaining > 0);
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
    public Map<String, Object> createLoan(int teamId, int equipmentId, int quantity, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("请选择借用日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("借用数量不合法");
        }

        Equipment equipment = equipmentDao.findByIdForUpdate(equipmentId);
        if (equipment == null) {
            throw new IllegalArgumentException("器材不存在");
        }
        if (equipment.getStatus() != null && "MAINTENANCE".equalsIgnoreCase(equipment.getStatus())) {
            throw new IllegalStateException("该器材维护中，暂不可借");
        }

        int totalQty = Math.max(0, equipment.getQuantity());
        if (quantity > totalQty) {
            throw new IllegalArgumentException("借用数量超过库存上限");
        }
        int usedQty = equipmentLoanDao.sumOverlappingActiveQuantity(equipmentId, startDate, endDate);
        if (usedQty + quantity > totalQty) {
            throw new IllegalStateException("该时间段库存不足，请减少数量或选择其他日期");
        }

        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days <= 0) {
            throw new IllegalArgumentException("借用天数不合法");
        }

        int cost = Math.max(0, equipment.getRatePerDay()) * days * quantity;
        int affected = teamDao.deductBalanceIfEnough(teamId, cost);
        if (affected <= 0) {
            throw new IllegalStateException("余额不足，无法提交借用申请");
        }

        EquipmentLoan loan = new EquipmentLoan();
        loan.setTeamId(teamId);
        loan.setEquipmentId(equipmentId);
        loan.setQuantity(quantity);
        loan.setStartDate(startDate);
        loan.setEndDate(endDate);
        loan.setStatus("PENDING");
        loan.setHeldCost(cost);
        loan.setFinalCost(0);
        equipmentLoanDao.create(loan);

        String desc = String.format("器材借用预扣：%s x%d（%s~%s）", equipment.getEquipmentName(), quantity, startDate, endDate);
        transactionService.record(teamId, "EQUIPMENT_HOLD", -cost, desc, loan.getId(), "equipment_loan");

        logger.info("提交器材借用申请: teamId={}, equipmentId={}, days={}, cost={}, loanId={}",
                teamId, equipmentId, days, cost, loan.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("loan", loan);
        data.put("days", days);
        data.put("cost", cost);
        return data;
    }

    @Transactional
    public Map<String, Object> cancelLoan(int teamId, int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        if (locked.getTeamId() != teamId) {
            throw new IllegalStateException("无权限操作该借用记录");
        }
        String st = locked.getStatus() == null ? "" : locked.getStatus().trim().toUpperCase();
        if (!("PENDING".equals(st) || "APPROVED".equals(st))) {
            throw new IllegalStateException("当前状态不可取消");
        }
        LocalDate today = LocalDate.now();
        if (locked.getStartDate() != null && !today.isBefore(locked.getStartDate())) {
            throw new IllegalStateException("已到预约时间，无法取消");
        }

        int updated = equipmentLoanDao.markCancelled(loanId);
        if (updated <= 0) {
            throw new IllegalStateException("取消失败，请重试");
        }

        int refund = Math.max(0, locked.getHeldCost());
        if (refund > 0) {
            int added = teamDao.addBalance(teamId, refund);
            if (added > 0) {
                Equipment eq = equipmentDao.findById(locked.getEquipmentId());
                String name = eq != null ? eq.getEquipmentName() : "器材";
                String desc = String.format("器材借用取消退款：%s x%d（%s~%s）", name, Math.max(1, locked.getQuantity()), locked.getStartDate(), locked.getEndDate());
                transactionService.record(teamId, "EQUIPMENT_HOLD", refund, desc, locked.getId(), "equipment_loan");
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("loan", equipmentLoanDao.findById(loanId));
        data.put("refund", refund);
        return data;
    }

    @Transactional
    public EquipmentLoan adminMarkApproved(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        if (!"PENDING".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可审核通过");
        }
        int updated = equipmentLoanDao.markApproved(loanId);
        if (updated <= 0) {
            throw new IllegalStateException("更新失败，请重试");
        }
        return equipmentLoanDao.findById(loanId);
    }

    @Transactional
    public Map<String, Object> adminReject(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        String st = locked.getStatus() == null ? "" : locked.getStatus().trim().toUpperCase();
        if (!("PENDING".equals(st) || "APPROVED".equals(st))) {
            throw new IllegalStateException("当前状态不可拒绝");
        }

        int updated = equipmentLoanDao.markRejected(loanId);
        if (updated <= 0) {
            throw new IllegalStateException("更新失败，请重试");
        }

        int refund = Math.max(0, locked.getHeldCost());
        if (refund > 0) {
            int added = teamDao.addBalance(locked.getTeamId(), refund);
            if (added > 0) {
                Equipment eq = equipmentDao.findById(locked.getEquipmentId());
                String name = eq != null ? eq.getEquipmentName() : "器材";
                String desc = String.format("器材借用拒绝退款：%s x%d（%s~%s）", name, Math.max(1, locked.getQuantity()), locked.getStartDate(), locked.getEndDate());
                transactionService.record(locked.getTeamId(), "EQUIPMENT_HOLD", refund, desc, locked.getId(), "equipment_loan");
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("loan", equipmentLoanDao.findById(loanId));
        data.put("refund", refund);
        return data;
    }

    /**
     * 24h 超时过期（仅 PENDING 且未到 start_date）：标记 EXPIRED 并退款
     */
    @Transactional
    public int autoExpirePendingLoans(LocalDate nowDate, Timestamp deadline) {
        int processed = 0;
        List<Integer> ids = equipmentLoanDao.listExpiredCandidateIds(deadline, 200);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        for (Integer id : ids) {
            if (id == null) continue;
            try {
                EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(id);
                if (locked == null) continue;
                if (!"PENDING".equalsIgnoreCase(locked.getStatus())) continue;
                if (locked.getStartDate() != null && !nowDate.isBefore(locked.getStartDate())) {
                    // 已到预约时间：不自动过期（避免到点自动释放）
                    continue;
                }

                int updated = equipmentLoanDao.markExpired(id);
                if (updated <= 0) continue;

                int refund = Math.max(0, locked.getHeldCost());
                if (refund > 0) {
                    int added = teamDao.addBalance(locked.getTeamId(), refund);
                    if (added > 0) {
                        Equipment eq = equipmentDao.findById(locked.getEquipmentId());
                        String name = eq != null ? eq.getEquipmentName() : "器材";
                        String desc = String.format("器材借用超时过期退款：%s x%d（%s~%s）", name, Math.max(1, locked.getQuantity()), locked.getStartDate(), locked.getEndDate());
                        transactionService.record(locked.getTeamId(), "EQUIPMENT_HOLD", refund, desc, locked.getId(), "equipment_loan");
                    }
                }
                processed++;
            } catch (Exception ex) {
                logger.error("器材借用自动过期处理失败: loanId={}", id, ex);
            }
        }
        return processed;
    }

    @Transactional
    public EquipmentLoan adminMarkBorrowed(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        String st = locked.getStatus() == null ? "" : locked.getStatus().trim().toUpperCase();
        if (!("PENDING".equals(st) || "APPROVED".equals(st))) {
            throw new IllegalStateException("当前状态不可借出");
        }

        // 借出时扣减“当前可用库存”（线下发放实体器材）
        Equipment equipment = equipmentDao.findByIdForUpdate(locked.getEquipmentId());
        if (equipment == null) {
            throw new IllegalArgumentException("器材不存在");
        }
        int qty = Math.max(1, locked.getQuantity());
        int invUpdated = equipmentDao.updateAvailableQuantityDelta(equipment.getId(), -qty);
        if (invUpdated <= 0) {
            throw new IllegalStateException("当前可用库存不足，无法借出");
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
    public Map<String, Object> adminMarkReturned(int loanId) {
        EquipmentLoan locked = equipmentLoanDao.findByIdForUpdate(loanId);
        if (locked == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }
        if (!"BORROWED".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("当前状态不可归还");
        }

        Equipment equipment = equipmentDao.findByIdForUpdate(locked.getEquipmentId());
        if (equipment == null) {
            throw new IllegalArgumentException("器材不存在");
        }

        int qty = Math.max(1, locked.getQuantity());
        int ratePerDay = Math.max(0, equipment.getRatePerDay());

        LocalDate planStart = locked.getStartDate();
        LocalDate planEnd = locked.getEndDate();
        LocalDate borrowedDate = toLocalDate(locked.getBorrowedAt());
        LocalDate today = LocalDate.now();

        LocalDate actualStart = planStart;
        if (borrowedDate != null && (actualStart == null || borrowedDate.isAfter(actualStart))) {
            actualStart = borrowedDate;
        }
        if (actualStart == null) {
            actualStart = today;
        }
        LocalDate actualEnd = today;

        int actualDays = (int) ChronoUnit.DAYS.between(actualStart, actualEnd) + 1;
        actualDays = Math.max(1, actualDays);
        int finalCost = ratePerDay * qty * actualDays;

        int held = Math.max(0, locked.getHeldCost());
        int delta = finalCost - held;

        // 超期补扣（delta>0）
        if (delta > 0) {
            int affected = teamDao.deductBalanceIfEnough(locked.getTeamId(), delta);
            if (affected <= 0) {
                throw new IllegalStateException("余额不足，无法归还结算（需补扣）");
            }
            String desc = String.format("器材借用超期补扣：%s x%d（%s~%s）", equipment.getEquipmentName(), qty, planStart, planEnd);
            transactionService.record(locked.getTeamId(), "EQUIPMENT_HOLD", -delta, desc, locked.getId(), "equipment_loan");
        }

        // 提前归还退款（delta<0）
        int refund = 0;
        if (delta < 0) {
            refund = -delta;
            int added = teamDao.addBalance(locked.getTeamId(), refund);
            if (added > 0) {
                String desc = String.format("器材借用提前归还退款：%s x%d（%s~%s）", equipment.getEquipmentName(), qty, planStart, planEnd);
                transactionService.record(locked.getTeamId(), "EQUIPMENT_HOLD", refund, desc, locked.getId(), "equipment_loan");
            }
        }

        int updated = equipmentLoanDao.markReturned(loanId, finalCost);
        if (updated <= 0) {
            throw new IllegalStateException("更新失败，请重试");
        }

        // 归还时释放“当前可用库存”
        equipmentDao.updateAvailableQuantityDelta(equipment.getId(), qty);

        EquipmentLoan latest = equipmentLoanDao.findById(loanId);
        logger.info("管理员标记已归还: loanId={}, teamId={}, equipmentId={}, held={}, final={}, refund={}, extra={}",
                loanId, latest.getTeamId(), latest.getEquipmentId(), held, finalCost, refund, Math.max(0, delta));

        Map<String, Object> data = new HashMap<>();
        data.put("loan", latest);
        data.put("heldCost", held);
        data.put("finalCost", finalCost);
        data.put("refund", refund);
        data.put("extraCost", Math.max(0, delta));
        return data;
    }

    public List<EquipmentLoan> adminListByStatus(String status) {
        String st = (status == null || status.trim().isEmpty()) ? "PENDING" : status.trim().toUpperCase();
        List<EquipmentLoan> list = equipmentLoanDao.adminListByStatus(st);
        logger.info("管理员查询器材借用: status={}, count={}", st, list != null ? list.size() : 0);
        return list;
    }

    private static LocalDate toLocalDate(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
