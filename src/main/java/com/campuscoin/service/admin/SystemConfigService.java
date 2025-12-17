package com.campuscoin.service.admin;

import com.campuscoin.dao.SystemConfigDao;
import com.campuscoin.model.SystemConfig;
import com.campuscoin.util.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 系统配置管理服务
 */
@Service
public class SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    @Autowired
    private SystemConfigDao systemConfigDao;

    @Autowired
    private ConfigCache configCache;

    /**
     * 获取所有配置（按分类分组）
     */
    public Map<String, List<SystemConfig>> getAllGrouped() {
        List<SystemConfig> configs = systemConfigDao.listAll();
        Map<String, List<SystemConfig>> grouped = new LinkedHashMap<>();
        
        // 按分类分组
        for (SystemConfig config : configs) {
            grouped.computeIfAbsent(config.getCategory(), k -> new ArrayList<>()).add(config);
        }
        
        logger.info("查询系统配置：共 {} 个分类", grouped.size());
        return grouped;
    }

    /**
     * 根据分类获取配置
     */
    public List<SystemConfig> getByCategory(String category) {
        return systemConfigDao.listByCategory(category);
    }

    /**
     * 根据key获取配置
     */
    public SystemConfig getByKey(String configKey) {
        return systemConfigDao.findByKey(configKey);
    }

    /**
     * 获取配置值（从缓存）
     */
    public String getValue(String configKey) {
        return configCache.get(configKey);
    }

    /**
     * 获取配置值（整数，从缓存）
     */
    public Integer getIntValue(String configKey) {
        return configCache.getInt(configKey);
    }

    /**
     * 获取配置值（整数，带默认值，从缓存）
     */
    public int getIntValue(String configKey, int defaultValue) {
        return configCache.getInt(configKey, defaultValue);
    }

    /**
     * 更新单个配置
     */
    @Transactional
    public void updateConfig(Integer id, String configValue, String updatedBy) {
        SystemConfig config = systemConfigDao.findById(id);
        if (config == null) {
            throw new IllegalArgumentException("配置项不存在: id=" + id);
        }

        // 验证值的有效性
        validateConfigValue(config, configValue);

        // 更新数据库
        int updated = systemConfigDao.updateValue(id, configValue, updatedBy);
        if (updated <= 0) {
            throw new RuntimeException("更新配置失败");
        }

        // 更新缓存
        configCache.update(config.getConfigKey(), configValue);

        logger.info("配置更新成功: {} = {} (by {})", config.getConfigKey(), configValue, updatedBy);
    }

    /**
     * 批量更新配置
     */
    @Transactional
    public void batchUpdateConfigs(List<SystemConfig> configs, String updatedBy) {
        if (configs == null || configs.isEmpty()) {
            return;
        }

        // 验证所有配置值
        for (SystemConfig config : configs) {
            SystemConfig dbConfig = systemConfigDao.findById(config.getId());
            if (dbConfig != null) {
                validateConfigValue(dbConfig, config.getConfigValue());
            }
        }

        // 批量更新数据库
        for (SystemConfig config : configs) {
            systemConfigDao.updateValue(config.getId(), config.getConfigValue(), updatedBy);
        }

        // 刷新缓存
        configCache.refresh();

        logger.info("批量更新配置成功：共 {} 项 (by {})", configs.size(), updatedBy);
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        configCache.refresh();
        logger.info("配置缓存手动刷新");
    }

    /**
     * 验证配置值的有效性
     */
    private void validateConfigValue(SystemConfig config, String value) {
        if ("NUMBER".equals(config.getValueType())) {
            try {
                int intValue = Integer.parseInt(value);
                
                // 检查最小值
                if (config.getMinValue() != null && intValue < config.getMinValue()) {
                    throw new IllegalArgumentException(
                        String.format("配置 %s 的值 %d 小于最小值 %d", 
                            config.getDisplayName(), intValue, config.getMinValue())
                    );
                }
                
                // 检查最大值
                if (config.getMaxValue() != null && intValue > config.getMaxValue()) {
                    throw new IllegalArgumentException(
                        String.format("配置 %s 的值 %d 大于最大值 %d", 
                            config.getDisplayName(), intValue, config.getMaxValue())
                    );
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("配置 " + config.getDisplayName() + " 必须是数字");
            }
        }
    }

    /**
     * 创建新配置
     */
    @Transactional
    public SystemConfig createConfig(SystemConfig config, String createdBy) {
        config.setUpdatedBy(createdBy);
        systemConfigDao.create(config);
        configCache.refresh();
        logger.info("创建配置成功: {} (by {})", config.getConfigKey(), createdBy);
        return config;
    }

    /**
     * 删除配置（逻辑删除）
     */
    @Transactional
    public void deleteConfig(Integer id, String deletedBy) {
        systemConfigDao.disable(id, deletedBy);
        configCache.refresh();
        logger.info("删除配置成功: id={} (by {})", id, deletedBy);
    }
}
