package com.campuscoin.controller.admin;

import com.campuscoin.dao.admin.AdminEquipmentLoanDao;
import com.campuscoin.model.EquipmentLoan;
import com.campuscoin.model.admin.AdminEquipmentLoanView;
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
    private final AdminEquipmentLoanDao adminEquipmentLoanDao;

    public AdminEquipmentController(EquipmentService equipmentService, AdminEquipmentLoanDao adminEquipmentLoanDao) {
        this.equipmentService = equipmentService;
        this.adminEquipmentLoanDao = adminEquipmentLoanDao;
    }

    @GetMapping("/loans")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(@RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int pageSize,
                                                                 HttpServletRequest request) {
        String st = status == null ? "" : status.trim().toUpperCase();
        // 支持“总览”：status 为空或 ALL 时不过滤状态
        String statusFilter = (st.isEmpty() || "ALL".equals(st)) ? null : st;
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(200, pageSize));
        int offset = (safePage - 1) * safeSize;

        List<AdminEquipmentLoanView> list = adminEquipmentLoanDao.listPaged(statusFilter, offset, safeSize);
        int total = adminEquipmentLoanDao.count(statusFilter);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", safePage);
        data.put("pageSize", safeSize);

        logger.info("管理员查询器材借用: admin={}, status={}, page={}, pageSize={}, returned={}, total={}",
                getAdminUser(request), statusFilter == null ? "ALL" : statusFilter, safePage, safeSize, list != null ? list.size() : 0, total);

        return ResponseEntity.ok(ApiResponse.ok("获取成功", data));
    }

    @PostMapping("/loans/{id}/approved")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markApproved(@PathVariable int id, HttpServletRequest request) {
        String adminUser = getAdminUser(request);
        try {
            EquipmentLoan updated = equipmentService.adminMarkApproved(id);
            Map<String, Object> data = new HashMap<>();
            data.put("loan", updated);
            return ResponseEntity.ok(ApiResponse.ok("已审核通过", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员审核通过异常: id={}, admin={}", id, adminUser, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    @PostMapping("/loans/{id}/rejected")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(@PathVariable int id, HttpServletRequest request) {
        String adminUser = getAdminUser(request);
        try {
            Map<String, Object> data = equipmentService.adminReject(id);
            return ResponseEntity.ok(ApiResponse.ok("已拒绝", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员拒绝异常: id={}, admin={}", id, adminUser, ex);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
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

    @PostMapping("/loans/{id}/returned")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markReturned(@PathVariable int id,
                                                                         HttpServletRequest request) {
        String adminUser = getAdminUser(request);
        try {
            Map<String, Object> data = equipmentService.adminMarkReturned(id);
            return ResponseEntity.ok(ApiResponse.ok("已归还", data));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
        } catch (Exception ex) {
            logger.error("管理员标记归还异常: id={}, admin={}", id, adminUser, ex);
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
