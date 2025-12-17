package com.campuscoin.controller.admin;

import com.campuscoin.model.admin.AdminAchievementListItem;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.payload.PagedResponse;
import com.campuscoin.service.admin.AdminAchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 成果审核控制器（管理员后台）
 */
@RestController
@RequestMapping("/api/admin/achievements")
public class AdminAchievementController {
    private static final Logger logger = LoggerFactory.getLogger(AdminAchievementController.class);

    private final AdminAchievementService adminAchievementService;

    public AdminAchievementController(AdminAchievementService adminAchievementService) {
        this.adminAchievementService = adminAchievementService;
    }

    /**
     * 分页查询成果列表（带筛选条件）
     * GET /api/admin/achievements?status=PENDING&category=论文&teamName=xxx&page=1&pageSize=20
     */
    @GetMapping
    public ApiResponse<PagedResponse<AdminAchievementListItem>> listAchievements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String teamName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        logger.info("管理员查询成果列表 - 状态:{}, 类型:{}, 团队:{}, 页码:{}, 每页:{}",
                status, category, teamName, page, pageSize);

        try {
            PagedResponse<AdminAchievementListItem> result = adminAchievementService.listPaged(status, category, teamName, page, pageSize);
            return ApiResponse.ok("查询成功", result);
        } catch (Exception e) {
            logger.error("查询成果列表失败", e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取成果详情
     * GET /api/admin/achievements/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<AdminAchievementListItem> getAchievementDetail(@PathVariable Integer id) {
        logger.info("管理员查询成果详情 - ID:{}", id);

        try {
            AdminAchievementListItem detail = adminAchievementService.getDetail(id);
            if (detail == null) {
                return ApiResponse.fail("成果不存在");
            }
            return ApiResponse.ok("查询成功", detail);
        } catch (Exception e) {
            logger.error("查询成果详情失败 - ID:{}", id, e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 审核通过成果（自动发币）
     * POST /api/admin/achievements/{id}/approve
     * Body: { "rewardCoins": 100 }
     */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approveAchievement(@PathVariable Integer id,
                                                @RequestBody ApproveRequest request,
                                                HttpServletRequest httpRequest) {
        String adminUsername = (String) httpRequest.getAttribute("adminUsername");
        logger.info("管理员审核通过成果 - ID:{}, 奖励币值:{}, 审核人:{}", id, request.getRewardCoins(), adminUsername);

        if (request.getRewardCoins() == null || request.getRewardCoins() <= 0) {
            return ApiResponse.fail("奖励币值必须大于0");
        }

        try {
            boolean success = adminAchievementService.approveAchievement(id, request.getRewardCoins(), adminUsername);
            if (success) {
                return ApiResponse.ok("审核通过并发币成功", null);
            } else {
                return ApiResponse.fail("审核失败：成果不存在或已审核");
            }
        } catch (Exception e) {
            logger.error("审核通过成果失败 - ID:{}", id, e);
            return ApiResponse.fail("审核失败：" + e.getMessage());
        }
    }

    /**
     * 审核驳回成果
     * POST /api/admin/achievements/{id}/reject
     * Body: { "rejectReason": "材料不符合要求" }
     */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> rejectAchievement(@PathVariable Integer id,
                                               @RequestBody RejectRequest request,
                                               HttpServletRequest httpRequest) {
        String adminUsername = (String) httpRequest.getAttribute("adminUsername");
        logger.info("管理员驳回成果 - ID:{}, 驳回原因:{}, 审核人:{}", id, request.getRejectReason(), adminUsername);

        if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
            return ApiResponse.fail("驳回原因不能为空");
        }

        try {
            boolean success = adminAchievementService.rejectAchievement(id, request.getRejectReason(), adminUsername);
            if (success) {
                return ApiResponse.ok("驳回成功", null);
            } else {
                return ApiResponse.fail("驳回失败：成果不存在或已审核");
            }
        } catch (Exception e) {
            logger.error("驳回成果失败 - ID:{}", id, e);
            return ApiResponse.fail("驳回失败：" + e.getMessage());
        }
    }

    /**
     * 审核通过请求体
     */
    public static class ApproveRequest {
        private Integer rewardCoins;

        public Integer getRewardCoins() {
            return rewardCoins;
        }

        public void setRewardCoins(Integer rewardCoins) {
            this.rewardCoins = rewardCoins;
        }
    }

    /**
     * 审核驳回请求体
     */
    public static class RejectRequest {
        private String rejectReason;

        public String getRejectReason() {
            return rejectReason;
        }

        public void setRejectReason(String rejectReason) {
            this.rejectReason = rejectReason;
        }
    }
}
