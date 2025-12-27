package com.campuscoin.service;

import com.campuscoin.dao.DeviceBookingDao;
import com.campuscoin.dao.DeviceDao;
import com.campuscoin.dao.TeamDao;
import com.campuscoin.model.Device;
import com.campuscoin.model.DeviceBooking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceDao deviceDao;
    private final DeviceBookingDao bookingDao;
    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public DeviceService(DeviceDao deviceDao, DeviceBookingDao bookingDao, TeamDao teamDao, TransactionService transactionService) {
        this.deviceDao = deviceDao;
        this.bookingDao = bookingDao;
        this.teamDao = teamDao;
        this.transactionService = transactionService;
    }

    public List<Device> listDevices(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null) {
            List<Device> list = deviceDao.listAllWithAvailability(startAt, endAt);
            for (Device d : list) {
                // MyBatis 可能把 0/1 映射到 boolean，这里兜底：维护中一律不可用
                if ("MAINTENANCE".equalsIgnoreCase(d.getRuntimeStatus())) {
                    d.setAvailable(false);
                }
            }
            return list;
        }

        List<Device> list = deviceDao.listAllWithRuntimeStatus();
        for (Device d : list) {
            d.setAvailable(!"MAINTENANCE".equalsIgnoreCase(d.getRuntimeStatus()));
        }
        return list;
    }

    private static boolean isBookableDeviceStatus(String status) {
        if (status == null) {
            return true;
        }
        String s = status.trim().toUpperCase();
        if (s.isEmpty()) {
            return true;
        }
        // 兼容历史数据：IDLE 代表可用
        return "AVAILABLE".equals(s) || "IDLE".equals(s);
    }

    private static String normalizeDeviceStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    @Transactional
    public DeviceBooking createBooking(int teamId, int deviceId, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("请选择预约时间段");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }

        // 限制一下单次预约最长 8 小时，避免误操作（可按需放开）
        long minutes = Duration.between(startAt, endAt).toMinutes();
        if (minutes <= 0) {
            throw new IllegalArgumentException("预约时长不合法");
        }
        if (minutes > 8 * 60L) {
            throw new IllegalArgumentException("单次最多预约 8 小时");
        }

        // 并发保护：锁住设备行，避免并发创建冲突预约
        Device locked = deviceDao.findByIdForUpdate(deviceId);
        if (locked == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (!isBookableDeviceStatus(locked.getStatus())) {
            String st = normalizeDeviceStatus(locked.getStatus());
            if ("BROKEN".equals(st)) {
                throw new IllegalStateException("设备故障，暂不可预约");
            }
            if ("IN_USE".equals(st)) {
                throw new IllegalStateException("设备使用中，暂不可预约");
            }
            throw new IllegalStateException("设备不可预约");
        }

        int overlap = bookingDao.countOverlappingActive(deviceId, startAt, endAt);
        if (overlap > 0) {
            throw new IllegalStateException("该设备在所选时间段已被预约/使用");
        }

        // 方案B：预约时预扣（按预约时长），结束结算时多退少补（仅退不补：以预约区间为上限）
        int ratePerHour = locked.getRatePerHour();
        int heldCost = (int) Math.ceil(minutes * ratePerHour / 60.0);
        if (heldCost < 0) heldCost = 0;

        int deducted = teamDao.deductBalanceIfEnough(teamId, heldCost);
        if (deducted <= 0) {
            throw new IllegalStateException("余额不足，无法预约");
        }

        DeviceBooking booking = new DeviceBooking();
        booking.setDeviceId(deviceId);
        booking.setTeamId(teamId);
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        booking.setStatus("RESERVED");
        // billed_cost 在方案B里作为“预扣金额(held)”，结束时会更新为“最终结算金额(final)”
        booking.setBilledCost(heldCost);

        bookingDao.create(booking);

        try {
            String desc = "设备预约预扣 " + locked.getDeviceName() + " (" + minutes + "分钟)";
            // 为了后台统计口径一致，这里统一使用 DEVICE_USAGE（负数代表支出预扣）
            transactionService.record(teamId, "DEVICE_USAGE", -heldCost, desc);
        } catch (Exception e) {
            logger.error("设备预约预扣流水记录失败: teamId={}, deviceId={}, heldCost={}", teamId, deviceId, heldCost, e);
        }

        logger.info("设备预约创建并预扣: teamId={}, deviceId={}, bookingId={}, startAt={}, endAt={}, heldCost={}",
                teamId, deviceId, booking.getId(), startAt, endAt, heldCost);
        return booking;
    }

    public DeviceBooking getMyActiveBooking(int teamId) {
        return bookingDao.findMyActive(teamId, LocalDateTime.now());
    }

    @Transactional
    public DeviceBooking startBooking(int teamId, int bookingId) {
        DeviceBooking lockedBooking = bookingDao.findByIdForUpdate(bookingId);
        if (lockedBooking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (lockedBooking.getTeamId() != teamId) {
            throw new IllegalStateException("无权限操作该预约");
        }
        if (!"RESERVED".equalsIgnoreCase(lockedBooking.getStatus())) {
            throw new IllegalStateException("当前状态不可开始使用");
        }

        // 维护检查（锁设备行，防止维护状态并发切换）
        Device lockedDevice = deviceDao.findByIdForUpdate(lockedBooking.getDeviceId());
        if (lockedDevice == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (!isBookableDeviceStatus(lockedDevice.getStatus())) {
            String st = normalizeDeviceStatus(lockedDevice.getStatus());
            if ("BROKEN".equals(st)) {
                throw new IllegalStateException("设备故障，无法开始使用");
            }
            if ("MAINTENANCE".equals(st)) {
                throw new IllegalStateException("设备维护中，无法开始使用");
            }
            if ("IN_USE".equals(st)) {
                // 这里与 booking.status=RESERVED 并存时，通常是后台手动置状态导致；仍然拦截
                throw new IllegalStateException("设备使用中，无法开始使用");
            }
            throw new IllegalStateException("设备不可用，无法开始使用");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = lockedBooking.getStartAt();
        LocalDateTime endAt = lockedBooking.getEndAt();

        // 允许提前 15 分钟开始，避免卡死
        if (now.isBefore(startAt.minusMinutes(15))) {
            throw new IllegalStateException("未到预约开始时间");
        }
        if (!now.isBefore(endAt)) {
            throw new IllegalStateException("预约已过期，请重新预约");
        }

        int updated = bookingDao.start(bookingId);
        if (updated <= 0) {
            throw new IllegalStateException("开始失败，请稍后重试");
        }

        DeviceBooking started = bookingDao.findById(bookingId);
        logger.info("设备开始使用: teamId={}, bookingId={}, deviceId={}", teamId, bookingId, lockedBooking.getDeviceId());
        return started;
    }

    @Transactional
    public FinishResult finishBooking(int teamId, int bookingId) {
        DeviceBooking lockedBooking = bookingDao.findByIdForUpdate(bookingId);
        if (lockedBooking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (lockedBooking.getTeamId() != teamId) {
            throw new IllegalStateException("无权限操作该预约");
        }
        if (!"IN_USE".equalsIgnoreCase(lockedBooking.getStatus())) {
            throw new IllegalStateException("当前状态不可结束结算");
        }
        if (lockedBooking.getActualStartAt() == null) {
            throw new IllegalStateException("开始时间缺失，无法结算");
        }

        Device lockedDevice = deviceDao.findByIdForUpdate(lockedBooking.getDeviceId());
        if (lockedDevice == null) {
            throw new IllegalArgumentException("设备不存在");
        }

        // 方案B：最终费用按“实际使用时长”计算，但以预约区间为上限（只退不补）
        LocalDateTime now = LocalDateTime.now();
        Settlement settlement = settleWithinReservation(lockedBooking, lockedDevice, now, false);

        int updated = bookingDao.finish(bookingId, settlement.finalCost);
        if (updated <= 0) {
            throw new IllegalStateException("结算失败，请稍后重试");
        }

        // 退款（若实际使用少于预约时长）
        if (settlement.refund > 0) {
            int added = teamDao.addBalance(teamId, settlement.refund);
            if (added > 0) {
                try {
                    String desc = "设备使用结算退款 " + lockedDevice.getDeviceName() + " (-" + settlement.refund + "币)";
                    transactionService.record(teamId, "DEVICE_USAGE", settlement.refund, desc);
                } catch (Exception e) {
                    logger.error("设备结算退款流水记录失败: teamId={}, bookingId={}, refund={}", teamId, bookingId, settlement.refund, e);
                }
            } else {
                logger.warn("设备结算退款入账失败: teamId={}, bookingId={}, refund={}", teamId, bookingId, settlement.refund);
            }
        }

        DeviceBooking finished = bookingDao.findById(bookingId);
        logger.info("设备结束结算: teamId={}, bookingId={}, deviceId={}, minutes={}, heldCost={}, finalCost={}, refund={}",
                teamId, bookingId, lockedDevice.getId(), settlement.chargeMinutes, settlement.heldCost, settlement.finalCost, settlement.refund);

        FinishResult result = new FinishResult();
        result.booking = finished;
        result.minutes = settlement.chargeMinutes;
        result.cost = settlement.finalCost;
        result.heldCost = settlement.heldCost;
        result.refund = settlement.refund;
        return result;
    }

    /**
     * 定时任务自动结束：对所有到点的 IN_USE 预约做结算（方案B：只退不补）。
     * 注意：本方法会循环批量处理，直到本轮没有到点记录或达到保护上限。
     */
    @Transactional
    public AutoFinishResult autoFinishDueBookings(LocalDateTime now) {
        int processed = 0;
        int refundedCount = 0;
        long refundedTotal = 0;
        List<Integer> failedIds = new ArrayList<>();

        // 防止极端情况下单次任务跑太久
        int safetyMax = 500;
        while (processed < safetyMax) {
            List<Integer> ids = bookingDao.listDueFinishIds(now, 50);
            if (ids == null || ids.isEmpty()) {
                break;
            }

            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                try {
                    AutoOneResult r = autoFinishOne(id);
                    processed++;
                    if (r.refund > 0) {
                        refundedCount++;
                        refundedTotal += r.refund;
                    }
                } catch (Exception ex) {
                    failedIds.add(id);
                    processed++;
                    logger.error("设备预约自动结束结算失败: bookingId={}", id, ex);
                }
                if (processed >= safetyMax) {
                    break;
                }
            }
        }

        AutoFinishResult out = new AutoFinishResult();
        out.processed = processed;
        out.refundedCount = refundedCount;
        out.refundedTotal = refundedTotal;
        out.failedBookingIds = failedIds;
        return out;
    }

    private AutoOneResult autoFinishOne(int bookingId) {
        DeviceBooking lockedBooking = bookingDao.findByIdForUpdate(bookingId);
        if (lockedBooking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!"IN_USE".equalsIgnoreCase(lockedBooking.getStatus())) {
            // 被人工结束/重复处理，忽略
            AutoOneResult r = new AutoOneResult();
            r.refund = 0;
            return r;
        }

        Device device = deviceDao.findByIdForUpdate(lockedBooking.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }

        // 自动结束以 end_at 为结束时间（强一致）
        LocalDateTime actualEnd = lockedBooking.getEndAt() != null ? lockedBooking.getEndAt() : LocalDateTime.now();
        Settlement settlement = settleWithinReservation(lockedBooking, device, actualEnd, true);

        int updated = bookingDao.finishAt(bookingId, settlement.finalCost, actualEnd);
        if (updated <= 0) {
            throw new IllegalStateException("自动结束更新失败");
        }

        if (settlement.refund > 0) {
            int added = teamDao.addBalance(lockedBooking.getTeamId(), settlement.refund);
            if (added > 0) {
                try {
                    String desc = "设备使用结算退款(自动) " + device.getDeviceName() + " (-" + settlement.refund + "币)";
                    transactionService.record(lockedBooking.getTeamId(), "DEVICE_USAGE", settlement.refund, desc);
                } catch (Exception e) {
                    logger.error("设备自动结算退款流水记录失败: teamId={}, bookingId={}, refund={}",
                            lockedBooking.getTeamId(), bookingId, settlement.refund, e);
                }
            } else {
                logger.warn("设备自动结算退款入账失败: teamId={}, bookingId={}, refund={}",
                        lockedBooking.getTeamId(), bookingId, settlement.refund);
            }
        }

        AutoOneResult r = new AutoOneResult();
        r.refund = settlement.refund;
        return r;
    }

    private static class Settlement {
        int heldCost;
        int finalCost;
        int refund;
        long chargeMinutes;
    }

    private static Settlement settleWithinReservation(DeviceBooking booking, Device device, LocalDateTime actualEnd, boolean auto) {
        LocalDateTime startAt = booking.getStartAt();
        LocalDateTime endAt = booking.getEndAt();
        LocalDateTime actualStartAt = booking.getActualStartAt();

        // 保底：出现脏数据时仍能结算
        if (startAt == null) {
            startAt = actualStartAt != null ? actualStartAt : LocalDateTime.now();
        }
        if (endAt == null) {
            endAt = actualEnd != null ? actualEnd : LocalDateTime.now();
        }
        if (actualStartAt == null) {
            actualStartAt = startAt;
        }
        if (actualEnd == null) {
            actualEnd = auto ? endAt : LocalDateTime.now();
        }

        // 方案B：只退不补，因此计费窗口被裁剪到预约区间内
        LocalDateTime effectiveStart = actualStartAt.isAfter(startAt) ? actualStartAt : startAt;
        LocalDateTime effectiveEnd = actualEnd.isBefore(endAt) ? actualEnd : endAt;

        long minutes = Duration.between(effectiveStart, effectiveEnd).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }

        // 至少计 1 分钟，避免 0 费用体验（会退到接近 1 分钟的费用）
        long chargeMinutes = Math.max(1, minutes);
        int ratePerHour = device.getRatePerHour();
        int finalCost = (int) Math.ceil(chargeMinutes * ratePerHour / 60.0);
        if (finalCost < 0) finalCost = 0;

        int heldCost = Math.max(0, booking.getBilledCost());
        int refund = Math.max(0, heldCost - finalCost);

        Settlement s = new Settlement();
        s.heldCost = heldCost;
        s.finalCost = finalCost;
        s.refund = refund;
        s.chargeMinutes = chargeMinutes;
        return s;
    }

    public static class FinishResult {
        private DeviceBooking booking;
        private long minutes;
        private int cost;
        private int heldCost;
        private int refund;

        public DeviceBooking getBooking() {
            return booking;
        }

        public long getMinutes() {
            return minutes;
        }

        public int getCost() {
            return cost;
        }

        public int getHeldCost() {
            return heldCost;
        }

        public int getRefund() {
            return refund;
        }
    }

    public static class AutoFinishResult {
        private int processed;
        private int refundedCount;
        private long refundedTotal;
        private List<Integer> failedBookingIds;

        public int getProcessed() {
            return processed;
        }

        public int getRefundedCount() {
            return refundedCount;
        }

        public long getRefundedTotal() {
            return refundedTotal;
        }

        public List<Integer> getFailedBookingIds() {
            return failedBookingIds;
        }
    }

    private static class AutoOneResult {
        int refund;
    }
}
