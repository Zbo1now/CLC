package com.campuscoin.controller.admin;

import com.campuscoin.model.EquipmentLoan;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public AdminEquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<List<EquipmentLoan>>> list(@RequestParam(required = false) String status,
                                                                 HttpServletRequest request) {
        List<EquipmentLoan> list = equipmentService.adminListByStatus(status);
        logger.info("管理员查询设备借用: admin={}, status={}, count={}", getAdminUser(request), status, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/loans/{id}/borrowed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markBorrowed(@PathVariable int id,
                                                                         HttpServletRequest request) {
        String adminUser = getAdminUser(request);
        try {
            EquipmentLoan updated = equipmentService.adminMarkBorrowed(id);
            Map<String, Object> data = new HashMap<>();
            data.put("loan", updated);
            return ResponseEntity.ok(ApiResponse.ok("已借出", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员标记借出异常: id={}, admin={}", id, adminUser, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    private String getAdminUser(HttpServletRequest request) {
        Object displayName = request.getAttribute("adminDisplayName");
        if (displayName != null) {
            return displayName.toString();
        }
        Object username = request.getAttribute("adminUsername");
        return username != null ? username.toString() : "admin";
    }
}
