package com.campuscoin.controller;

import com.campuscoin.model.DutyTaskSignup;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.DutyTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/duty-tasks")
public class AdminDutyTaskController {

    private static final Logger logger = LoggerFactory.getLogger(AdminDutyTaskController.class);

    private final DutyTaskService dutyTaskService;

    @Value("${admin.token:}")
    private String adminToken;

    public AdminDutyTaskController(DutyTaskService dutyTaskService) {
        this.dutyTaskService = dutyTaskService;
    }

    // 管理员侧（暂不做 UI）：确认某条报名完成并发币
    @PostMapping("/signups/{signupId}/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirm(@PathVariable int signupId, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        String adminUser = getAdminUser(request);
        try {
            DutyTaskSignup updated = dutyTaskService.adminConfirmSignup(signupId, adminUser);
            Map<String, Object> data = new HashMap<>();
            data.put("signup", updated);
            return ResponseEntity.ok(ApiResponse.ok("已确认并发放奖励", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员确认值班异常: signupId={}", signupId, e);
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

    private String getAdminUser(HttpServletRequest request) {
        String u = request.getHeader("X-Admin-User");
        if (u == null || u.trim().isEmpty()) {
            return "admin";
        }
        return u.trim();
    }
}
