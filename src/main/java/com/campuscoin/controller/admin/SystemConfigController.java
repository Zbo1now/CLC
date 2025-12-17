package com.campuscoin.controller.admin;

import com.campuscoin.model.SystemConfig;
import com.campuscoin.payload.ApiResponse;
import com.campuscoin.service.admin.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 系统配置管理 Controller（后台管理）
 */
@RestController
@RequestMapping("/api/admin/system-configs")
public class SystemConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取所有配置（按分类分组）
     * GET /api/admin/system-configs
     */
    @GetMapping
    public ApiResponse<Map<String, List<SystemConfig>>> getAllConfigs(HttpServletRequest request) {
        try {
            String adminUsername = getAdminUsername(request);
            logger.info("管理员查询系统配置: admin={}", adminUsername);

            Map<String, List<SystemConfig>> configs = systemConfigService.getAllGrouped();
            return ApiResponse.ok("查询成功", configs);
        } catch (Exception e) {
            logger.error("查询系统配置失败", e);
            // 返回用户友好的错误信息
            String friendlyMessage = getFriendlyErrorMessage(e);
            return ApiResponse.fail(friendlyMessage);
        }
    }

    /**
     * 根据分类获取配置
     * GET /api/admin/system-configs/category/{category}
     */
    @GetMapping("/category/{category}")
    public ApiResponse<List<SystemConfig>> getConfigsByCategory(
            @PathVariable String category,
            HttpServletRequest request
    ) {
        try {
            logger.info("查询配置分类: category={}", category);
            List<SystemConfig> configs = systemConfigService.getByCategory(category);
            return ApiResponse.ok("查询成功", configs);
        } catch (Exception e) {
            logger.error("查询配置分类失败: category={}", category, e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取单个配置
     * GET /api/admin/system-configs/{configKey}
     */
    @GetMapping("/{configKey}")
    public ApiResponse<SystemConfig> getConfig(
            @PathVariable String configKey,
            HttpServletRequest request
    ) {
        try {
            SystemConfig config = systemConfigService.getByKey(configKey);
            if (config == null) {
                return ApiResponse.fail("配置项不存在");
            }
            return ApiResponse.ok("查询成功", config);
        } catch (Exception e) {
            logger.error("查询配置失败: configKey={}", configKey, e);
            return ApiResponse.fail("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新单个配置
     * PUT /api/admin/system-configs/{id}
     * Body: { "configValue": "100" }
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateConfig(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = getAdminUsername(request);
            String configValue = body.get("configValue");
            
            if (configValue == null) {
                return ApiResponse.fail("配置值不能为空");
            }

            systemConfigService.updateConfig(id, configValue, adminUsername);
            logger.info("配置更新成功: id={}, value={}, admin={}", id, configValue, adminUsername);
            
            return ApiResponse.ok("更新成功", null);
        } catch (IllegalArgumentException e) {
            logger.warn("配置更新参数错误: id={}, error={}", id, e.getMessage());
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            logger.error("配置更新失败: id={}", id, e);
            return ApiResponse.fail("更新失败：" + e.getMessage());
        }
    }

    /**
     * 批量更新配置
     * POST /api/admin/system-configs/batch
     * Body: [{ "id": 1, "configValue": "100" }, ...]
     */
    @PostMapping("/batch")
    public ApiResponse<Void> batchUpdateConfigs(
            @RequestBody List<SystemConfig> configs,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = getAdminUsername(request);
            systemConfigService.batchUpdateConfigs(configs, adminUsername);
            logger.info("批量更新配置成功: count={}, admin={}", configs.size(), adminUsername);
            
            return ApiResponse.ok("批量更新成功", null);
        } catch (IllegalArgumentException e) {
            logger.warn("批量更新配置参数错误: error={}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            logger.error("批量更新配置失败", e);
            return ApiResponse.fail("批量更新失败：" + e.getMessage());
        }
    }

    /**
     * 刷新缓存
     * POST /api/admin/system-configs/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<Void> refreshCache(HttpServletRequest request) {
        try {
            String adminUsername = getAdminUsername(request);
            systemConfigService.refreshCache();
            logger.info("配置缓存刷新成功: admin={}", adminUsername);
            
            return ApiResponse.ok("缓存刷新成功", null);
        } catch (Exception e) {
            logger.error("刷新缓存失败", e);
            return ApiResponse.fail("刷新失败：" + e.getMessage());
        }
    }

    /**
     * 创建新配置
     * POST /api/admin/system-configs
     */
    @PostMapping
    public ApiResponse<SystemConfig> createConfig(
            @RequestBody SystemConfig config,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = getAdminUsername(request);
            SystemConfig created = systemConfigService.createConfig(config, adminUsername);
            logger.info("创建配置成功: key={}, admin={}", config.getConfigKey(), adminUsername);
            
            return ApiResponse.ok("创建成功", created);
        } catch (Exception e) {
            logger.error("创建配置失败", e);
            return ApiResponse.fail("创建失败：" + e.getMessage());
        }
    }

    /**
     * 删除配置
     * DELETE /api/admin/system-configs/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(
            @PathVariable Integer id,
            HttpServletRequest request
    ) {
        try {
            String adminUsername = getAdminUsername(request);
            systemConfigService.deleteConfig(id, adminUsername);
            logger.info("删除配置成功: id={}, admin={}", id, adminUsername);
            
            return ApiResponse.ok("删除成功", null);
        } catch (Exception e) {
            logger.error("删除配置失败: id={}", id, e);
            return ApiResponse.fail("删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取管理员用户名
     */
    private String getAdminUsername(HttpServletRequest request) {
        Object adminAttr = request.getAttribute("adminUsername");
        return adminAttr != null ? adminAttr.toString() : "system";
    }

    /**
     * 将技术异常转换为用户友好的错误信息
     */
    private String getFriendlyErrorMessage(Exception e) {
        String message = e.getMessage();
        
        // SQL语法错误
        if (message != null && message.contains("SQL syntax")) {
            return "配置数据查询失败，系统配置异常，请联系技术人员";
        }
        
        // 数据库连接错误
        if (e instanceof java.sql.SQLException) {
            return "数据库连接异常，请稍后重试或联系管理员";
        }
        
        // 空指针异常
        if (e instanceof NullPointerException) {
            return "数据读取失败，请刷新页面重试";
        }
        
        // 非法参数异常
        if (e instanceof IllegalArgumentException) {
            return "参数错误：" + message;
        }
        
        // 其他未知异常
        return "操作失败，请稍后重试或联系管理员";
    }
}
