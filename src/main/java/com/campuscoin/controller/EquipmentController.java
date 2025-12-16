package com.campuscoin.controller;

import com.campuscoin.model.Equipment;
import com.campuscoin.model.EquipmentLoan;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.EquipmentService;
import com.campuscoin.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentController.class);

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Equipment>>> list(@RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            HttpServletRequest request,
                                                            HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        LocalDate s = parseDateNullable(startDate);
        LocalDate e = parseDateNullable(endDate);

        List<Equipment> list = equipmentService.listEquipments(s, e);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @GetMapping("/{equipmentId}/occupied")
    public ResponseEntity<ApiResponse<List<EquipmentLoan>>> occupied(@PathVariable int equipmentId,
                                                                    @RequestParam String startDate,
                                                                    @RequestParam String endDate,
                                                                    HttpServletRequest request,
                                                                    HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        LocalDate s = parseDate(startDate);
        LocalDate e = parseDate(endDate);
        List<EquipmentLoan> list = equipmentService.listOccupied(equipmentId, s, e);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/{equipmentId}/loans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@PathVariable int equipmentId,
                                                                   @RequestBody CreateLoanRequest req,
                                                                   HttpServletRequest request,
                                                                   HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        try {
            LocalDate s = parseDate(req != null ? req.getStartDate() : null);
            LocalDate e = parseDate(req != null ? req.getEndDate() : null);
            Map<String, Object> data = equipmentService.createLoan(teamId, equipmentId, s, e);
            return ResponseEntity.ok(ApiResponse.ok("申请成功", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("创建器材借用申请异常: equipmentId={}", equipmentId, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    @GetMapping("/me/loan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myActive(HttpServletRequest request, HttpSession session) {
        Integer teamId = SessionHelper.resolveTeamId(request, session);
        if (teamId == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("请先登录"));
        }

        Map<String, Object> data = equipmentService.getMyActive(teamId);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    private static LocalDate parseDateNullable(String v) {
        if (v == null || v.trim().isEmpty()) {
            return null;
        }
        return parseDate(v);
    }

    private static LocalDate parseDate(String v) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return LocalDate.parse(v.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static class CreateLoanRequest {
        private String startDate;
        private String endDate;

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }
    }
}
