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
        if ("MAINTENANCE".equalsIgnoreCase(locked.getStatus())) {
            throw new IllegalStateException("设备维护中，暂不可预约");
        }

        int overlap = bookingDao.countOverlappingActive(deviceId, startAt, endAt);
        if (overlap > 0) {
            throw new IllegalStateException("该设备在所选时间段已被预约/使用");
        }

        DeviceBooking booking = new DeviceBooking();
        booking.setDeviceId(deviceId);
        booking.setTeamId(teamId);
        booking.setStartAt(startAt);
        booking.setEndAt(endAt);
        booking.setStatus("RESERVED");
        booking.setBilledCost(0);

        bookingDao.create(booking);
        logger.info("设备预约创建: teamId={}, deviceId={}, bookingId={}, startAt={}, endAt={}", teamId, deviceId, booking.getId(), startAt, endAt);
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
        if ("MAINTENANCE".equalsIgnoreCase(lockedDevice.getStatus())) {
            throw new IllegalStateException("设备维护中，无法开始使用");
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

        LocalDateTime actualStart = lockedBooking.getActualStartAt();
        LocalDateTime actualEnd = LocalDateTime.now();

        long minutes = Duration.between(actualStart, actualEnd).toMinutes();
        if (minutes < 0) {
            minutes = 0;
        }

        // 至少计 1 分钟，避免 0 费用导致异常体验
        long chargeMinutes = Math.max(1, minutes);

        int ratePerHour = lockedDevice.getRatePerHour();
        int cost = (int) Math.ceil(chargeMinutes * ratePerHour / 60.0);
        if (cost < 0) {
            cost = 0;
        }

        int deducted = teamDao.deductBalanceIfEnough(teamId, cost);
        if (deducted <= 0) {
            throw new IllegalStateException("余额不足，无法结束结算");
        }

        int updated = bookingDao.finish(bookingId, cost);
        if (updated <= 0) {
            throw new IllegalStateException("结算失败，请稍后重试");
        }

        try {
            String desc = "设备使用 " + lockedDevice.getDeviceName() + " (" + chargeMinutes + "分钟)";
            transactionService.record(teamId, "DEVICE_USAGE", -cost, desc);
        } catch (Exception e) {
            logger.error("设备使用流水记录失败: teamId={}, bookingId={}, cost={}", teamId, bookingId, cost, e);
        }

        DeviceBooking finished = bookingDao.findById(bookingId);
        logger.info("设备结束结算: teamId={}, bookingId={}, deviceId={}, minutes={}, cost={}", teamId, bookingId, lockedDevice.getId(), chargeMinutes, cost);

        FinishResult result = new FinishResult();
        result.booking = finished;
        result.minutes = chargeMinutes;
        result.cost = cost;
        return result;
    }

    public static class FinishResult {
        private DeviceBooking booking;
        private long minutes;
        private int cost;

        public DeviceBooking getBooking() {
            return booking;
        }

        public long getMinutes() {
            return minutes;
        }

        public int getCost() {
            return cost;
        }
    }
}
