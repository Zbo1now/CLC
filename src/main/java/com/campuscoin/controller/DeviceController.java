package com.campuscoin.controller;

import com.campuscoin.dao.DeviceDao;
import com.campuscoin.model.Device;
import com.campuscoin.model.DeviceBooking;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.DeviceService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;
    private final DeviceDao deviceDao;

    public DeviceController(DeviceService deviceService, DeviceDao deviceDao) {
        this.deviceService = deviceService;
        this.deviceDao = deviceDao;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Device>>> listDevices(
            @RequestParam(value = "startAt", required = false) String startAt,
            @RequestParam(value = "endAt", required = false) String endAt,
            HttpServletRequest request,
            HttpSession session) {

        Integer teamId = SessionHelper.resolveTeamId(request, session);
        LocalDateTime start = parseLocalDateTimeOrNull(startAt);
        LocalDateTime end = parseLocalDateTimeOrNull(endAt);

        logger.info("查询设备列表: teamId={}, startAt={}, endAt={}", teamId, startAt, endAt);
        List<Device> list = deviceService.listDevices(start, end);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/{deviceId}/bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(@PathVariable int deviceId,
                                                                          @RequestBody BookingRequest body,
                                                                          HttpServletRequest request,
                                                                          HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            LocalDateTime startAt = parseLocalDateTimeRequired(body != null ? body.getStartAt() : null, "startAt");
            LocalDateTime endAt = parseLocalDateTimeRequired(body != null ? body.getEndAt() : null, "endAt");

            DeviceBooking booking = deviceService.createBooking(teamId, deviceId, startAt, endAt);
            Map<String, Object> data = new HashMap<>();
            data.put("booking", booking);
            return ResponseEntity.ok(ApiResponse.ok("预约成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("设备预约异常: teamId={}, deviceId={}", teamId, deviceId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("预约失败，请稍后重试"));
        }
    }

    @GetMapping("/me/booking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myActiveBooking(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            DeviceBooking booking = deviceService.getMyActiveBooking(teamId);
            Map<String, Object> data = new HashMap<>();
            data.put("booking", booking);
            data.put("device", booking != null ? deviceDao.findById(booking.getDeviceId()) : null);
            return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
        } catch (Exception e) {
            logger.error("查询我的设备预约异常: teamId={}", teamId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("获取失败，请稍后重试"));
        }
    }

    @PostMapping("/bookings/{bookingId}/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> start(@PathVariable int bookingId,
                                                                  HttpServletRequest request,
                                                                  HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            DeviceBooking booking = deviceService.startBooking(teamId, bookingId);
            Map<String, Object> data = new HashMap<>();
            data.put("booking", booking);
            return ResponseEntity.ok(ApiResponse.ok("开始使用", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("开始使用异常: teamId={}, bookingId={}", teamId, bookingId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("开始失败，请稍后重试"));
        }
    }

    @PostMapping("/bookings/{bookingId}/finish")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finish(@PathVariable int bookingId,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            DeviceService.FinishResult result = deviceService.finishBooking(teamId, bookingId);
            Map<String, Object> data = new HashMap<>();
            data.put("booking", result.getBooking());
            data.put("minutes", result.getMinutes());
            data.put("cost", result.getCost());
            data.put("heldCost", result.getHeldCost());
            data.put("refund", result.getRefund());
            return ResponseEntity.ok(ApiResponse.ok("结算成功", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("结束结算异常: teamId={}, bookingId={}", teamId, bookingId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("结算失败，请稍后重试"));
        }
    }

    public static class BookingRequest {
        private String startAt;
        private String endAt;

        public String getStartAt() {
            return startAt;
        }

        public void setStartAt(String startAt) {
            this.startAt = startAt;
        }

        public String getEndAt() {
            return endAt;
        }

        public void setEndAt(String endAt) {
            this.endAt = endAt;
        }
    }

    private static LocalDateTime parseLocalDateTimeOrNull(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return parseLocalDateTime(v.trim());
    }

    private static LocalDateTime parseLocalDateTimeRequired(String v, String fieldName) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        try {
            return parseLocalDateTime(v.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 格式不正确");
        }
    }

    private static LocalDateTime parseLocalDateTime(String input) {
        // 兼容："yyyy-MM-dd HH:mm" / "yyyy-MM-dd HH:mm:ss" / ISO "yyyy-MM-dd'T'HH:mm:ss"
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) {
        }
        try {
            return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignore) {
        }
        return LocalDateTime.parse(input, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
