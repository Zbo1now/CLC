package com.campuscoin.controller;

import com.campuscoin.model.AchievementSubmission;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.AchievementService;
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
@RequestMapping("/api/admin/achievements")
public class AdminAchievementController {

    private static final Logger logger = LoggerFactory.getLogger(AdminAchievementController.class);

    private final AchievementService achievementService;

    @Value("${admin.token:}")
    private String adminToken;

    public AdminAchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AchievementSubmission>>> list(@RequestParam(required = false) String status,
                                                                        HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        List<AchievementSubmission> list = achievementService.adminList(status);
        logger.info("管理员查询成果: status={}, count={}", status, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable int id,
                                                                    HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        String reviewedBy = getAdminUser(request);
        try {
            AchievementSubmission updated = achievementService.adminApprove(id, reviewedBy);
            Map<String, Object> data = new HashMap<>();
            data.put("submission", updated);
            return ResponseEntity.ok(ApiResponse.ok("审核通过", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员审核通过异常: id={}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(@PathVariable int id,
                                                                   @RequestBody(required = false) RejectRequest req,
                                                                   HttpServletRequest request) {
        if (!isAdmin(request)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("无权限"));
        }

        String reviewedBy = getAdminUser(request);
        String reason = req != null ? req.getReason() : null;

        try {
            AchievementSubmission updated = achievementService.adminReject(id, reviewedBy, reason);
            Map<String, Object> data = new HashMap<>();
            data.put("submission", updated);
            return ResponseEntity.ok(ApiResponse.ok("已驳回", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员驳回异常: id={}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        if (adminToken == null || adminToken.trim().isEmpty()) {
            // 未配置时直接禁用管理员接口，避免误暴露
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

    public static class RejectRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
