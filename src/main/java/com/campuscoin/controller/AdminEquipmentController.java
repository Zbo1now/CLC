package com.campuscoin.controller;

import com.campuscoin.model.EquipmentLoan;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/equipments")
public class AdminEquipmentController {

    private static final Logger logger = LoggerFactory.getLogger(AdminEquipmentController.class);

    private final EquipmentService equipmentService;

    @Value("${admin.token:}")
    private String adminToken;

    public AdminEquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<List<EquipmentLoan>>> list(@RequestParam(required = false) String status,
                                                                 HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        List<EquipmentLoan> list = equipmentService.adminListByStatus(status);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/loans/{id}/borrowed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markBorrowed(@PathVariable int id,
                                                                         HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        try {
            EquipmentLoan updated = equipmentService.adminMarkBorrowed(id);
            Map<String, Object> data = new HashMap<>();
            data.put("loan", updated);
            return ResponseEntity.ok(ApiResponse.ok("已借出", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员标记借出异常: id={}", id, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    @PostMapping("/loans/{id}/returned")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markReturned(@PathVariable int id,
                                                                         HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        try {
            EquipmentLoan updated = equipmentService.adminMarkReturned(id);
            Map<String, Object> data = new HashMap<>();
            data.put("loan", updated);
            return ResponseEntity.ok(ApiResponse.ok("已归还", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员标记归还异常: id={}", id, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        if (adminToken == null || adminToken.trim().isEmpty()) {
            logger.warn("管理员接口被禁用: 未配置 admin.token");
            return false;
        }
        String token = request.getHeader("X-Admin-Token");
        return token != null && token.equals(adminToken);
    }
}
