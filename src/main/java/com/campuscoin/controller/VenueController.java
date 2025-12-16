package com.campuscoin.controller;

import com.campuscoin.payload.CreateVenueBookingRequest;
import com.campuscoin.service.VenueService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Object> list(@RequestParam(value = "startTime", required = false) String startTime,
                                    @RequestParam(value = "endTime", required = false) String endTime,
                                    HttpServletRequest request,
                                    HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        Map<String, Object> resp = new HashMap<>();
        if (teamId == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }

        LocalDateTime st = parseDT(startTime);
        LocalDateTime et = parseDT(endTime);
        resp.put("success", true);
        resp.put("data", venueService.listVenues(st, et));
        return resp;
    }

    @GetMapping("/{venueId}/occupied")
    public Map<String, Object> occupied(@PathVariable("venueId") int venueId,
                                        @RequestParam("startTime") String startTime,
                                        @RequestParam("endTime") String endTime,
                                        HttpServletRequest request,
                                        HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        Map<String, Object> resp = new HashMap<>();
        if (teamId == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }

        LocalDateTime st = parseDT(startTime);
        LocalDateTime et = parseDT(endTime);
        if (st == null || et == null) {
            resp.put("success", false);
            resp.put("message", "时间格式不正确，示例：2025-12-20 14:00");
            return resp;
        }

        resp.put("success", true);
        resp.put("data", venueService.listOccupied(venueId, st, et));
        return resp;
    }

    @PostMapping("/{venueId}/bookings")
    public Map<String, Object> create(@PathVariable("venueId") int venueId,
                                      @RequestBody CreateVenueBookingRequest body,
                                      HttpServletRequest request,
                                      HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        Map<String, Object> resp = new HashMap<>();
        if (teamId == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }

        try {
            resp.put("success", true);
            resp.put("data", venueService.createBooking(teamId, venueId, body.getDate(), body.getStartTime(), body.getEndTime()));
            return resp;
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return resp;
        } catch (IllegalStateException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return resp;
        } catch (Exception e) {
            logger.error("创建场地预约异常: venueId={}", venueId, e);
            resp.put("success", false);
            resp.put("message", "服务器异常");
            return resp;
        }
    }

    @GetMapping("/me/booking")
    public Map<String, Object> myBooking(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        Map<String, Object> resp = new HashMap<>();
        if (teamId == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }

        resp.put("success", true);
        resp.put("data", venueService.getMyActiveBooking(teamId));
        return resp;
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public Map<String, Object> confirm(@PathVariable("bookingId") int bookingId, HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        Map<String, Object> resp = new HashMap<>();
        if (teamId == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }

        try {
            venueService.confirmInUse(teamId, bookingId);
            resp.put("success", true);
            resp.put("data", true);
            return resp;
        } catch (IllegalArgumentException e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return resp;
        } catch (Exception e) {
            logger.error("确认使用异常: bookingId={}", bookingId, e);
            resp.put("success", false);
            resp.put("message", "服务器异常");
            return resp;
        }
    }
}
