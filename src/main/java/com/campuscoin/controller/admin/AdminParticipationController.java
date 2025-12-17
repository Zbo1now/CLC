package com.campuscoin.controller.admin;

import com.campuscoin.model.ActivityParticipation;
import com.campuscoin.model.admin.AdminParticipationListItem;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.payload.PagedResponse;
import com.campuscoin.service.admin.AdminParticipationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动参与记录管理 Controller
 */
@RestController
@RequestMapping("/api/admin/participations")
public class AdminParticipationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminParticipationController.class);

    @Autowired
    private AdminParticipationService adminParticipationService;

    /**
     * 分页查询参与记录列表
     */
    @GetMapping
    public ApiResponse<PagedResponse<AdminParticipationListItem>> listPaged(
            @RequestParam(required = false) Integer activityId,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            PagedResponse<AdminParticipationListItem> response = adminParticipationService.listPaged(
                    activityId, reviewStatus, searchKeyword, page, pageSize
            );
            return ApiResponse.ok("查询成功", response);
        } catch (Exception e) {
            logger.error("查询参与记录列表失败", e);
            return ApiResponse.fail("查询失败，请稍后重试");
        }
    }

    /**
     * 获取参与记录详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ActivityParticipation> getDetail(@PathVariable Integer id) {
        try {
            ActivityParticipation participation = adminParticipationService.getDetail(id);
            if (participation == null) {
                return ApiResponse.fail("参与记录不存在");
            }
            return ApiResponse.ok("查询成功", participation);
        } catch (Exception e) {
            logger.error("获取参与记录详情失败: id={}", id, e);
            return ApiResponse.fail("查询失败，请稍后重试");
        }
    }

    /**
     * 审核通过（自动发币）
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");

            boolean success = adminParticipationService.approve(id, adminUsername);
            if (success) {
                return ApiResponse.ok("审核通过成功", null);
            } else {
                return ApiResponse.fail("审核通过失败，记录不存在或已审核或活动人数已满");
            }
        } catch (Exception e) {
            logger.error("审核通过失败: id={}", id, e);
            return ApiResponse.fail("审核通过失败: " + e.getMessage());
        }
    }

    /**
     * 审核驳回
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @PathVariable Integer id,
            @RequestBody Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            String rejectReason = params.get("rejectReason");

            if (rejectReason == null || rejectReason.trim().isEmpty()) {
                return ApiResponse.fail("驳回原因不能为空");
            }

            boolean success = adminParticipationService.reject(id, rejectReason, adminUsername);
            if (success) {
                return ApiResponse.ok("审核驳回成功", null);
            } else {
                return ApiResponse.fail("审核驳回失败，记录不存在或已审核");
            }
        } catch (Exception e) {
            logger.error("审核驳回失败: id={}", id, e);
            return ApiResponse.fail("审核驳回失败: " + e.getMessage());
        }
    }

    /**
     * 批量审核通过
     */
    @PostMapping("/batch-approve")
    public ApiResponse<Map<String, Integer>> batchApprove(
            @RequestBody Map<String, List<Integer>> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            List<Integer> ids = params.get("ids");

            if (ids == null || ids.isEmpty()) {
                return ApiResponse.fail("请选择要审核的记录");
            }

            int successCount = adminParticipationService.batchApprove(ids, adminUsername);
            int failCount = ids.size() - successCount;

            Map<String, Integer> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return ApiResponse.ok("批量审核完成", result);
        } catch (Exception e) {
            logger.error("批量审核失败", e);
            return ApiResponse.fail("批量审核失败，请稍后重试");
        }
    }

    /**
     * 标记已完成并发放奖励
     */
    @PostMapping("/{id}/complete")
    public ApiResponse<Void> markCompleted(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            String completionNotes = params != null ? params.get("completionNotes") : null;

            boolean success = adminParticipationService.markCompleted(id, completionNotes, adminUsername);
            if (success) {
                return ApiResponse.ok("标记已完成成功", null);
            } else {
                return ApiResponse.fail("标记已完成失败，记录不存在或未审核通过");
            }
        } catch (Exception e) {
            logger.error("标记已完成失败: id={}", id, e);
            return ApiResponse.fail("标记已完成失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记已完成
     */
    @PostMapping("/batch-complete")
    public ApiResponse<Map<String, Integer>> batchMarkCompleted(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) params.get("ids");
            String completionNotes = (String) params.get("completionNotes");

            if (ids == null || ids.isEmpty()) {
                return ApiResponse.fail("请选择要标记的记录");
            }

            int successCount = adminParticipationService.batchMarkCompleted(ids, completionNotes, adminUsername);
            int failCount = ids.size() - successCount;

            Map<String, Integer> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return ApiResponse.ok("批量标记完成", result);
        } catch (Exception e) {
            logger.error("批量标记已完成失败", e);
            return ApiResponse.fail("批量标记失败，请稍后重试");
        }
    }
}
