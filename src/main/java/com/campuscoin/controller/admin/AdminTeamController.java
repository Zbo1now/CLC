package com.campuscoin.controller.admin;

import com.campuscoin.model.admin.AdminTeamDetail;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.admin.AdminTeamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 团队管理 Controller（后台管理）
 * 提供团队列表、详情、管理操作等接口
 */
@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTeamController.class);

    @Autowired
    private AdminTeamService adminTeamService;

    /**
     * 分页查询团队列表
     * GET /api/admin/teams?keyword=xxx&orderBy=balance&orderDir=DESC&page=1&pageSize=20
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listTeams(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "createdAt") String orderBy,
            @RequestParam(required = false, defaultValue = "DESC") String orderDir,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize
    ) {
        try {
            logger.info("API: 查询团队列表 - keyword={}, orderBy={}, orderDir={}, page={}, pageSize={}",
                    keyword, orderBy, orderDir, page, pageSize);

            Map<String, Object> result = adminTeamService.listPaged(keyword, orderBy, orderDir, page, pageSize);
            return ApiResponse.ok("查询成功", result);
        } catch (Exception e) {
            logger.error("查询团队列表失败", e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取团队详情（含能力画像）
     * GET /api/admin/teams/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<AdminTeamDetail> getTeamDetail(@PathVariable Integer id) {
        try {
            logger.info("API: 查询团队详情 - teamId={}", id);

            AdminTeamDetail detail = adminTeamService.getTeamDetail(id);
            if (detail == null) {
                return ApiResponse.fail("团队不存在");
            }

            return ApiResponse.ok("查询成功", detail);
        } catch (Exception e) {
            logger.error("查询团队详情失败 - teamId={}", id, e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取团队行为时间线
     * GET /api/admin/teams/{id}/timeline
     */
    @GetMapping("/{id}/timeline")
    public ApiResponse<Map<String, Object>> getTeamTimeline(@PathVariable Integer id) {
        try {
            logger.info("API: 查询团队行为时间线 - teamId={}", id);

            Map<String, Object> timeline = adminTeamService.getTeamTimeline(id);
            return ApiResponse.ok("查询成功", timeline);
        } catch (Exception e) {
            logger.error("查询团队行为时间线失败 - teamId={}", id, e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 重置团队密码
     * POST /api/admin/teams/{id}/reset-password
     * Body: { "newPassword": "123456", "reason": "用户申请" }
     */
    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Integer id,
            @RequestBody Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            String newPassword = params.get("newPassword");
            String reason = params.get("reason");

            if (newPassword == null || newPassword.length() < 6) {
                return ApiResponse.fail("新密码长度至少6位");
            }

            // 从 session 中获取管理员 ID（简化处理，实际需要从 AdminSession 中获取）
            Integer adminId = 1; // TODO: 从 session 获取真实管理员 ID
            String ipAddress = request.getRemoteAddr();

            logger.info("API: 重置团队密码 - teamId={}, adminId={}, reason={}", id, adminId, reason);

            boolean success = adminTeamService.resetPassword(id, newPassword, adminId, ipAddress, reason);
            if (success) {
                return ApiResponse.ok("密码重置成功", null);
            } else {
                return ApiResponse.fail("密码重置失败");
            }
        } catch (Exception e) {
            logger.error("重置团队密码失败 - teamId={}", id, e);
            return ApiResponse.fail("操作失败：" + e.getMessage());
        }
    }

    /**
     * 手动调整团队虚拟币
     * POST /api/admin/teams/{id}/adjust-balance
     * Body: { "amount": 100, "reason": "活动补偿" }
     */
    @PostMapping("/{id}/adjust-balance")
    public ApiResponse<Void> adjustBalance(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request
    ) {
        try {
            Integer amount = (Integer) params.get("amount");
            String reason = (String) params.get("reason");

            if (amount == null || amount == 0) {
                return ApiResponse.fail("调整金额不能为0");
            }

            // 从 session 中获取管理员 ID
            Integer adminId = 1; // TODO: 从 session 获取真实管理员 ID
            String ipAddress = request.getRemoteAddr();

            logger.info("API: 手动调整虚拟币 - teamId={}, amount={}, adminId={}, reason={}", id, amount, adminId, reason);

            boolean success = adminTeamService.adjustBalance(id, amount, reason, adminId, ipAddress);
            if (success) {
                return ApiResponse.ok("调整成功", null);
            } else {
                return ApiResponse.fail("调整失败");
            }
        } catch (Exception e) {
            logger.error("手动调整虚拟币失败 - teamId={}", id, e);
            return ApiResponse.fail("操作失败：" + e.getMessage());
        }
    }
}
