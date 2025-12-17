package com.campuscoin.controller.admin;

import com.campuscoin.model.AchievementSubmission;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public AdminAchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AchievementSubmission>>> list(@RequestParam(required = false) String status,
                                                                        HttpServletRequest request) {
        List<AchievementSubmission> list = achievementService.adminList(status);
        logger.info("管理员查询成果: admin={}, status={}, count={}", getAdminUser(request), status, list != null ? list.size() : 0);
        return ResponseEntity.ok(ApiResponse.ok("获取成功", list));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable int id,
                                                                    HttpServletRequest request) {
        String reviewedBy = getAdminUser(request);
        try {
            AchievementSubmission updated = achievementService.adminApprove(id, reviewedBy);
            Map<String, Object> data = new HashMap<>();
            data.put("submission", updated);
            return ResponseEntity.ok(ApiResponse.ok("审核通过", data));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员审核通过异常: id={}, admin={}", id, reviewedBy, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("操作失败，请稍后重试"));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(@PathVariable int id,
                                                                   @RequestBody(required = false) RejectRequest req,
                                                                   HttpServletRequest request) {
        String reviewedBy = getAdminUser(request);
        String reason = req != null ? req.getReason() : null;

        try {
            AchievementSubmission updated = achievementService.adminReject(id, reviewedBy, reason);
            Map<String, Object> data = new HashMap<>();
            data.put("submission", updated);
            return ResponseEntity.ok(ApiResponse.ok("已驳回", data));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            logger.error("管理员驳回异常: id={}, admin={}", id, reviewedBy, e);
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
