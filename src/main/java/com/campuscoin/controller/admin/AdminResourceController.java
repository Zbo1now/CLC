package com.campuscoin.controller.admin;

import com.campuscoin.payload.ApiResponse;
import com.campuscoin.payload.PagedResponse;
import com.campuscoin.service.admin.AdminResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 资源管理Controller（工位、设备、器材、场地）
 */
@RestController
@RequestMapping("/api/admin/resources")
public class AdminResourceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminResourceController.class);

    @Autowired
    private AdminResourceService resourceService;

    /**
     * 获取资源统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(HttpServletRequest request) {
        try {
            Map<String, Object> stats = resourceService.getResourceStats();
            return ApiResponse.ok("获取统计成功", stats);
        } catch (Exception e) {
            logger.error("获取资源统计失败", e);
            return ApiResponse.fail("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询资源列表
     * @param type 资源类型：workstation, device, equipment, venue
     */
    @GetMapping("/{type}")
    public ApiResponse<PagedResponse<?>> listResources(
            @PathVariable String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        try {
            PagedResponse<?> result = resourceService.listResources(type, status, keyword, page, pageSize);
            return ApiResponse.ok("查询成功", result);
        } catch (Exception e) {
            logger.error("查询资源列表失败: type={}", type, e);
            return ApiResponse.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取资源详情
     */
    @GetMapping("/{type}/{id}")
    public ApiResponse<Map<String, Object>> getResourceDetail(
            @PathVariable String type,
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        try {
            Map<String, Object> resource = resourceService.getResourceDetail(type, id);
            if (resource == null) {
                return ApiResponse.fail("资源不存在");
            }
            return ApiResponse.ok("获取成功", resource);
        } catch (Exception e) {
            logger.error("获取资源详情失败: type={}, id={}", type, id, e);
            return ApiResponse.fail("获取失败: " + e.getMessage());
        }
    }

    /**
     * 创建资源
     */
    @PostMapping("/{type}")
    public ApiResponse<Void> createResource(
            @PathVariable String type,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            boolean success = resourceService.createResource(type, params, adminUsername);
            if (success) {
                return ApiResponse.ok("创建成功", null);
            } else {
                return ApiResponse.fail("创建失败");
            }
        } catch (Exception e) {
            logger.error("创建资源失败: type={}", type, e);
            return ApiResponse.fail("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新资源
     */
    @PutMapping("/{type}/{id}")
    public ApiResponse<Void> updateResource(
            @PathVariable String type,
            @PathVariable Integer id,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            boolean success = resourceService.updateResource(type, id, params, adminUsername);
            if (success) {
                return ApiResponse.ok("更新成功", null);
            } else {
                return ApiResponse.fail("更新失败，资源可能不存在");
            }
        } catch (Exception e) {
            logger.error("更新资源失败: type={}, id={}", type, id, e);
            return ApiResponse.fail("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除资源
     */
    @DeleteMapping("/{type}/{id}")
    public ApiResponse<Void> deleteResource(
            @PathVariable String type,
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = (String) request.getAttribute("admin_username");
            boolean success = resourceService.deleteResource(type, id, adminUsername);
            if (success) {
                return ApiResponse.ok("删除成功", null);
            } else {
                return ApiResponse.fail("删除失败，资源可能不存在或正在被使用");
            }
        } catch (Exception e) {
            logger.error("删除资源失败: type={}, id={}", type, id, e);
            return ApiResponse.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新资源状态
     */
    @PatchMapping("/{type}/{id}/status")
    public ApiResponse<Void> updateResourceStatus(
            @PathVariable String type,
            @PathVariable Integer id,
            @RequestBody Map<String, String> params,
            HttpServletRequest request
    ) {
        try {
            String status = params.get("status");
            String adminUsername = (String) request.getAttribute("admin_username");
            boolean success = resourceService.updateResourceStatus(type, id, status, adminUsername);
            if (success) {
                return ApiResponse.ok("状态更新成功", null);
            } else {
                return ApiResponse.fail("状态更新失败");
            }
        } catch (Exception e) {
            logger.error("更新资源状态失败: type={}, id={}", type, id, e);
            return ApiResponse.fail("状态更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取资源使用记录
     */
    @GetMapping("/{type}/{id}/history")
    public ApiResponse<PagedResponse<?>> getResourceHistory(
            @PathVariable String type,
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request
    ) {
        try {
            PagedResponse<?> history = resourceService.getResourceHistory(type, id, page, pageSize);
            return ApiResponse.ok("获取成功", history);
        } catch (Exception e) {
            logger.error("获取资源历史记录失败: type={}, id={}", type, id, e);
            return ApiResponse.fail("获取失败: " + e.getMessage());
        }
    }
}
