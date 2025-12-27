package com.campuscoin.controller.admin;

import com.campuscoin.model.Activity;
import com.campuscoin.model.admin.AdminActivityListItem;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.payload.PagedResponse;
import com.campuscoin.service.admin.AdminActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 活动管理 Controller
 */
@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private static final Logger logger = LoggerFactory.getLogger(AdminActivityController.class);

    @Autowired
    private AdminActivityService adminActivityService;

    /**
     * 分页查询活动列表
     */
    @GetMapping
    public ApiResponse<PagedResponse<AdminActivityListItem>> listPaged(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            PagedResponse<AdminActivityListItem> response = adminActivityService.listPaged(
                    activityType, status, searchKeyword, page, pageSize
            );
            return ApiResponse.ok("查询成功", response);
        } catch (Exception e) {
            logger.error("查询活动列表失败", e);
            return ApiResponse.fail("查询失败，请稍后重试");
        }
    }

    /**
     * 获取活动详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Activity> getDetail(@PathVariable Integer id) {
        try {
            Activity activity = adminActivityService.getDetail(id);
            if (activity == null) {
                return ApiResponse.fail("活动不存在");
            }
            return ApiResponse.ok("查询成功", activity);
        } catch (Exception e) {
            logger.error("获取活动详情失败: id={}", id, e);
            return ApiResponse.fail("查询失败，请稍后重试");
        }
    }

    /**
     * 创建活动
     */
    @PostMapping
    public ApiResponse<Activity> create(
            @RequestBody Activity activity,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("adminUsername");

            // 校验必填字段
            if (activity.getActivityName() == null || activity.getActivityName().trim().isEmpty()) {
                return ApiResponse.fail("活动名称不能为空");
            }
            if (activity.getActivityType() == null) {
                return ApiResponse.fail("活动类型不能为空");
            }
            if (activity.getStartTime() == null) {
                return ApiResponse.fail("开始时间不能为空");
            }
            if (activity.getEndTime() == null) {
                return ApiResponse.fail("结束时间不能为空");
            }
            if (activity.getStartTime().after(activity.getEndTime())) {
                return ApiResponse.fail("开始时间不能晚于结束时间");
            }
            if (activity.getRewardCoins() == null || activity.getRewardCoins() < 0) {
                return ApiResponse.fail("奖励币值不能为负数");
            }
            if (activity.getMaxParticipants() == null || activity.getMaxParticipants() <= 0) {
                return ApiResponse.fail("最大参与人数必须大于0");
            }

            Activity created = adminActivityService.create(activity, adminUsername);
            return ApiResponse.ok("创建成功", created);
        } catch (Exception e) {
            logger.error("创建活动失败", e);
            return ApiResponse.fail("创建失败，请检查输入信息后重试");
        }
    }

    /**
     * 更新活动
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Integer id,
            @RequestBody Activity activity,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("adminUsername");

            // 校验必填字段
            if (activity.getActivityName() == null || activity.getActivityName().trim().isEmpty()) {
                return ApiResponse.fail("活动名称不能为空");
            }
            if (activity.getStartTime() != null && activity.getEndTime() != null
                    && activity.getStartTime().after(activity.getEndTime())) {
                return ApiResponse.fail("开始时间不能晚于结束时间");
            }
            if (activity.getRewardCoins() != null && activity.getRewardCoins() < 0) {
                return ApiResponse.fail("奖励币值不能为负数");
            }
            if (activity.getMaxParticipants() != null && activity.getMaxParticipants() <= 0) {
                return ApiResponse.fail("最大参与人数必须大于0");
            }

            boolean success = adminActivityService.update(id, activity, adminUsername);
            if (success) {
                return ApiResponse.ok("更新成功", null);
            } else {
                return ApiResponse.fail("更新失败，活动不存在或已开始");
            }
        } catch (Exception e) {
            logger.error("更新活动失败: id={}", id, e);
            return ApiResponse.fail("更新失败，请稍后重试");
        }
    }

    /**
     * 更新活动状态（下线、取消）
     */
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("adminUsername");

            // 校验状态值
            if (!"CANCELLED".equals(status) && !"FINISHED".equals(status)) {
                return ApiResponse.fail("无效的状态值");
            }

            boolean success = adminActivityService.updateStatus(id, status, adminUsername);
            if (success) {
                return ApiResponse.ok("操作成功", null);
            } else {
                return ApiResponse.fail("操作失败，活动不存在");
            }
        } catch (Exception e) {
            logger.error("更新活动状态失败: id={}, status={}", id, status, e);
            return ApiResponse.fail("操作失败，请稍后重试");
        }
    }

    /**
     * 删除活动（仅未开始的活动）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("adminUsername");

            boolean success = adminActivityService.delete(id, adminUsername);
            if (success) {
                return ApiResponse.ok("删除成功", null);
            } else {
                return ApiResponse.fail("删除失败，活动不存在或已开始");
            }
        } catch (Exception e) {
            logger.error("删除活动失败: id={}", id, e);
            return ApiResponse.fail("删除失败，请稍后重试");
        }
    }
}
