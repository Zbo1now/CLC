package com.campuscoin.controller.admin;

import com.campuscoin.model.DutyTaskSignup;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.DutyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/duty-tasks")
public class AdminDutyTaskController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDutyTaskController.class);

    private final DutyTaskService dutyTaskService;

    public AdminDutyTaskController(DutyTaskService dutyTaskService) {
        this.dutyTaskService = dutyTaskService;
    }

    @PostMapping("/signups/{signupId}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirm(@PathVariable int signupId, HttpServletRequest request) {
        String adminUser = getAdminUser(request);
        try {
            DutyTaskSignup updated = dutyTaskService.adminConfirmSignup(signupId, adminUser);
            Map<String, Object> data = new HashMap<>();
            data.put("signup", updated);
            return ResponseEntity.ok(ApiResponse.ok("已确认并发放奖励", data));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员确认值班异常: signupId={}, admin={}", signupId, adminUser, e);
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
