package com.campuscoin.controller;

import com.campuscoin.payload.CreateVenueBookingRequest;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.VenueService;
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

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private static final Logger logger = LoggerFactory.getLogger(VenueController.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    private LocalDateTime parseDT(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return LocalDateTime.parse(s, DT_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(@RequestParam(value = "startTime", required = false) String startTime,
                                                    @RequestParam(value = "endTime", required = false) String endTime,
                                                    HttpServletRequest request,
                                                    HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        LocalDateTime st = parseDT(startTime);
        LocalDateTime et = parseDT(endTime);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", venueService.listVenues(st, et)));
    }

    @GetMapping("/{venueId}/occupied")
    public ResponseEntity<ApiResponse<Object>> occupied(@PathVariable("venueId") int venueId,
                                                        @RequestParam("startTime") String startTime,
                                                        @RequestParam("endTime") String endTime,
                                                        HttpServletRequest request,
                                                        HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        LocalDateTime st = parseDT(startTime);
        LocalDateTime et = parseDT(endTime);
        if (st == null || et == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("时间格式不正确，示例：2025-12-20 14:00"));
        }

        return ResponseEntity.ok(ApiResponse.ok("获取成功", venueService.listOccupied(venueId, st, et)));
    }

    @PostMapping("/{venueId}/bookings")
    public ResponseEntity<ApiResponse<Object>> create(@PathVariable("venueId") int venueId,
                                                      @RequestBody CreateVenueBookingRequest body,
                                                      HttpServletRequest request,
                                                      HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            return ResponseEntity.ok(ApiResponse.ok("预约成功",
                    venueService.createBooking(teamId, venueId, body.getDate(), body.getStartTime(), body.getEndTime())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建场地预约异常: venueId={}", venueId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("服务器异常"));
        }
    }

    @GetMapping("/me/booking")
    public ResponseEntity<ApiResponse<Object>> myBooking(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        return ResponseEntity.ok(ApiResponse.ok("获取成功", venueService.getMyActiveBooking(teamId)));
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<Object>> confirm(@PathVariable("bookingId") int bookingId,
                                                       HttpServletRequest request,
                                                       HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            venueService.confirmInUse(teamId, bookingId);
            return ResponseEntity.ok(ApiResponse.ok("确认成功", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("确认使用异常: bookingId={}", bookingId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("服务器异常"));
        }
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Object>> cancel(@PathVariable("bookingId") int bookingId,
                                                      HttpServletRequest request,
                                                      HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            return ResponseEntity.ok(ApiResponse.ok("取消成功", venueService.cancelBooking(teamId, bookingId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("取消场地预约异常: bookingId={}", bookingId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("服务器异常"));
        }
    }
}
