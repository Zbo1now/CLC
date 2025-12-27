package com.campuscoin.service;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.VenueBookingDao;
import com.campuscoin.dao.VenueDao;
import com.campuscoin.model.Venue;
import com.campuscoin.model.VenueBooking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class VenueService {

    private static final Logger logger = LoggerFactory.getLogger(VenueService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final VenueDao venueDao;
    private final VenueBookingDao venueBookingDao;
    private final TeamDao teamDao;
    private final TransactionService transactionService;

    public VenueService(VenueDao venueDao, VenueBookingDao venueBookingDao, TeamDao teamDao, TransactionService transactionService) {
        this.venueDao = venueDao;
        this.venueBookingDao = venueBookingDao;
        this.teamDao = teamDao;
        this.transactionService = transactionService;
    }

    public List<Venue> listVenues(LocalDateTime startTime, LocalDateTime endTime) {
        List<Venue> list = venueDao.listAll();
        LocalDateTime now = LocalDateTime.now();

        if (startTime == null || endTime == null) {
            for (Venue v : list) {
                boolean maintenance = v.getStatus() != null && v.getStatus().equalsIgnoreCase("MAINTENANCE");
                v.setAvailable(!maintenance);

                if (maintenance) {
                    v.setCurrentState("MAINTENANCE");
                } else {
                    String cur = venueBookingDao.findCurrentStatus(v.getId(), now);
                    v.setCurrentState(cur == null ? "FREE" : cur.toUpperCase());
                }
            }
            logger.info("查询场地列表(未传时间段): returned={}", list.size());
            return list;
        }

        for (Venue v : list) {
            boolean maintenance = v.getStatus() != null && v.getStatus().equalsIgnoreCase("MAINTENANCE");
            if (maintenance) {
                v.setAvailable(false);
                v.setCurrentState("MAINTENANCE");
                continue;
            }
            int cnt = venueBookingDao.countOverlappingActive(v.getId(), startTime, endTime);
            v.setAvailable(cnt == 0);

            String cur = venueBookingDao.findCurrentStatus(v.getId(), now);
            v.setCurrentState(cur == null ? "FREE" : cur.toUpperCase());
        }

        logger.info("查询场地列表: start={}, end={}, returned={}", startTime, endTime, list.size());
        return list;
    }

    public List<Map<String, Object>> listOccupied(int venueId, LocalDateTime startTime, LocalDateTime endTime) {
        List<VenueBooking> list = venueBookingDao.listOverlappingActive(venueId, startTime, endTime);
        List<Map<String, Object>> out = new ArrayList<>();
        for (VenueBooking b : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("startTime", b.getStartTime());
            m.put("endTime", b.getEndTime());
            m.put("status", b.getStatus());
            out.add(m);
        }
        logger.info("查询场地占用: venueId={}, start={}, end={}, returned={}", venueId, startTime, endTime, out.size());
        return out;
    }

    public Map<String, Object> getMyActiveBooking(int teamId) {
        VenueBooking booking = venueBookingDao.findMyLatest(teamId);
        if (booking == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("booking", null);
            data.put("venue", null);
            return data;
        }
        Venue venue = venueDao.findById(booking.getVenueId());
        Map<String, Object> data = new HashMap<>();
        data.put("booking", booking);
        data.put("venue", venue);
        return data;
    }

    public LocalDateTime parseDateTime(String date, String time) {
        if (date == null || time == null) return null;
        try {
            LocalDate d = LocalDate.parse(date, DATE_FMT);
            LocalTime t = LocalTime.parse(time, TIME_FMT);
            return LocalDateTime.of(d, t);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Transactional
    public VenueBooking createBooking(int teamId, int venueId, String date, String startTimeStr, String endTimeStr) {
        LocalDateTime startTime = parseDateTime(date, startTimeStr);
        LocalDateTime endTime = parseDateTime(date, endTimeStr);
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("日期或时间格式不正确");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }

        // 仅允许整小时，避免计费歧义
        if (startTime.getMinute() != 0 || endTime.getMinute() != 0) {
            throw new IllegalArgumentException("仅支持整点预约（分钟必须为 00）");
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw new IllegalArgumentException("时间段不合法");
        }
        long hours = minutes / 60;
        if (hours <= 0 || minutes % 60 != 0) {
            throw new IllegalArgumentException("仅支持整小时预约");
        }

        Venue venue = venueDao.findByIdForUpdate(venueId);
        if (venue == null) {
            throw new IllegalArgumentException("场地不存在");
        }
        if (venue.getStatus() != null && venue.getStatus().equalsIgnoreCase("MAINTENANCE")) {
            throw new IllegalArgumentException("场地维护中，暂不可预约");
        }

        int overlap = venueBookingDao.countOverlappingActive(venueId, startTime, endTime);
        if (overlap > 0) {
            throw new IllegalArgumentException("该时间段已被预约");
        }

        int rate = Math.max(0, venue.getRatePerHour() == null ? 0 : venue.getRatePerHour());
        int cost = Math.toIntExact(hours * (long) rate);

        int affected = teamDao.deductBalanceIfEnough(teamId, cost);
        if (affected <= 0) {
            throw new IllegalStateException("余额不足");
        }

        VenueBooking booking = new VenueBooking();
        booking.setTeamId(teamId);
        booking.setVenueId(venueId);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus("BOOKED");
        booking.setHeldCost(cost);
        venueBookingDao.create(booking);

        String desc = String.format("场地短租预扣：%s %s %s-%s（%d小时）",
                venue.getVenueName(), date, startTimeStr, endTimeStr, hours);
        transactionService.record(teamId, "VENUE_HOLD", -cost, desc);

        logger.info("创建场地预约: teamId={}, venueId={}, start={}, end={}, hours={}, cost={}, bookingId={}",
                teamId, venueId, startTime, endTime, hours, cost, booking.getId());
        return booking;
    }

    @Transactional
    public void confirmInUse(int teamId, int bookingId) {
        VenueBooking booking = venueBookingDao.findByIdForUpdate(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!Objects.equals(booking.getTeamId(), teamId)) {
            throw new IllegalArgumentException("无权限操作该预约");
        }
        if (!"BOOKED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("当前状态不可确认");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = booking.getStartTime();
        if (now.isBefore(start)) {
            throw new IllegalArgumentException("未到开始时间，暂不可确认");
        }
        if (now.isAfter(start.plusMinutes(10))) {
            throw new IllegalArgumentException("已超过确认时间窗口，预约将自动取消");
        }

        int updated = venueBookingDao.markInUse(bookingId);
        if (updated <= 0) {
            throw new IllegalStateException("确认失败");
        }

        logger.info("确认使用场地: teamId={}, bookingId={}", teamId, bookingId);
    }

    @Transactional
    public int autoCancelExpiredBooked(int limit) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(10);
        List<VenueBooking> list = venueBookingDao.listBookedBefore(deadline, Math.max(1, limit));
        int cancelled = 0;
        for (VenueBooking b : list) {
            // 规则：预约开始后 10 分钟内未确认 => 自动取消（不退款）
            if (b.getStartTime() != null && b.getStartTime().isAfter(deadline)) {
                continue;
            }
            int updated = venueBookingDao.markCancelled(b.getId(), "AUTO_CANCELLED");
            if (updated > 0) {
                cancelled++;
                logger.info("自动取消场地预约: bookingId={}, teamId={}, venueId={}, start={}, end={}, heldCost={}（不退款）",
                        b.getId(), b.getTeamId(), b.getVenueId(), b.getStartTime(), b.getEndTime(), b.getHeldCost());
            }
        }
        return cancelled;
    }

    /**
     * 到点强制结束（防止超时继续占用）：
     * - BOOKED 到 end_time 仍未确认：标记 AUTO_CANCELLED（不退款）
     * - IN_USE 超过 end_time：标记 COMPLETED
     */
    @Transactional
    public Map<String, Integer> autoFinishDueBookings(int limit) {
        int safeLimit = Math.max(1, Math.min(500, limit));
        LocalDateTime now = LocalDateTime.now();

        int autoCancelled = 0;
        List<VenueBooking> bookedEnded = venueBookingDao.listBookedEndedBefore(now, safeLimit);
        for (VenueBooking b : bookedEnded) {
            int updated = venueBookingDao.markCancelled(b.getId(), "AUTO_CANCELLED");
            if (updated > 0) {
                autoCancelled++;
                logger.info("到点强制结束(未确认自动取消): bookingId={}, teamId={}, venueId={}, start={}, end={}, heldCost={}（不退款）",
                        b.getId(), b.getTeamId(), b.getVenueId(), b.getStartTime(), b.getEndTime(), b.getHeldCost());
            }
        }

        int completed = 0;
        List<VenueBooking> inUseEnded = venueBookingDao.listInUseEndedBefore(now, safeLimit);
        for (VenueBooking b : inUseEnded) {
            int updated = venueBookingDao.markCompleted(b.getId());
            if (updated > 0) {
                completed++;
                logger.info("到点强制结束(使用中自动结束): bookingId={}, teamId={}, venueId={}, start={}, end={}",
                        b.getId(), b.getTeamId(), b.getVenueId(), b.getStartTime(), b.getEndTime());
            }
        }

        Map<String, Integer> out = new HashMap<>();
        out.put("autoCancelled", autoCancelled);
        out.put("completed", completed);
        return out;
    }

    /**
     * 用户取消预约：仅允许开始前取消，取消后全额退款。
     */
    @Transactional
    public Map<String, Object> cancelBooking(int teamId, int bookingId) {
        VenueBooking booking = venueBookingDao.findByIdForUpdate(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("预约不存在");
        }
        if (!Objects.equals(booking.getTeamId(), teamId)) {
            throw new IllegalArgumentException("无权限操作该预约");
        }
        if (!"BOOKED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("当前状态不可取消");
        }
        LocalDateTime now = LocalDateTime.now();
        if (booking.getStartTime() != null && !now.isBefore(booking.getStartTime())) {
            throw new IllegalArgumentException("已到开始时间，无法取消");
        }

        int updated = venueBookingDao.markUserCancelled(bookingId);
        if (updated <= 0) {
            throw new IllegalStateException("取消失败，请稍后重试");
        }

        int refund = Math.max(0, booking.getHeldCost() == null ? 0 : booking.getHeldCost());
        if (refund > 0) {
            teamDao.addBalance(teamId, refund);
            Venue venue = venueDao.findById(booking.getVenueId());
            String name = venue != null ? venue.getVenueName() : "场地";
            String desc = String.format("场地短租取消退款：%s %s-%s", name,
                    booking.getStartTime() != null ? booking.getStartTime().toLocalTime().toString() : "",
                    booking.getEndTime() != null ? booking.getEndTime().toLocalTime().toString() : "");
            transactionService.record(teamId, "VENUE_HOLD", refund, desc, bookingId, "venue_booking");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("booking", venueBookingDao.findById(bookingId));
        data.put("refund", refund);
        return data;
    }
}
