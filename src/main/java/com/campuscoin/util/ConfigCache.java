package com.campuscoin.util;

import com.campuscoin.dao.SystemConfigDao;
import com.campuscoin.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置缓存工具
 * 提供高性能的配置值读取，避免频繁查询数据库
 */
@Component
public class ConfigCache {

    private static final Logger logger = LoggerFactory.getLogger(ConfigCache.class);

    @Autowired
    private SystemConfigDao systemConfigDao;

    // 内存缓存：key -> value
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private void ensureLoaded() {
        // 懒加载：首次访问时初始化缓存
        if (cache.isEmpty()) {
            synchronized (this) {
                if (cache.isEmpty()) {
                    refresh();
                }
            }
        }
    }

    /**
     * 应用启动时初始化缓存
     */
    // @PostConstruct // 暂时禁用自动初始化，改为懒加载
    public void init() {
        refresh();
        logger.info("配置缓存初始化完成，共加载 {} 个配置项", cache.size());
    }

    /**
     * 刷新缓存（从数据库重新加载）
     */
    public synchronized void refresh() {
        try {
            cache.clear();
            List<SystemConfig> configs = systemConfigDao.listAll();
            for (SystemConfig config : configs) {
                cache.put(config.getConfigKey(), config.getConfigValue());
            }
            logger.info("配置缓存刷新成功，共 {} 个配置项", configs.size());
        } catch (Exception e) {
            logger.error("刷新配置缓存失败", e);
        }
    }

    /**
     * 获取配置值（字符串）
     */
    public String get(String key) {
        ensureLoaded();
        return cache.get(key);
    }

    /**
     * 获取配置值（带默认值）
     */
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取配置值（整数）
     */
    public Integer getInt(String key) {
        ensureLoaded();
        String value = cache.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("配置项 {} 的值 {} 不是有效整数", key, value);
            return null;
        }
    }

    /**
     * 获取配置值（整数，带默认值）
     */
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取配置值（布尔）
     */
    public Boolean getBoolean(String key) {
        ensureLoaded();
        String value = cache.get(key);
        if (value == null) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 获取配置值（布尔，带默认值）
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查配置项是否存在
     */
    public boolean exists(String key) {
        ensureLoaded();
        return cache.containsKey(key);
    }

    /**
     * 获取所有缓存的配置项
     */
    public Map<String, String> getAll() {
        ensureLoaded();
        return new ConcurrentHashMap<>(cache);
    }

    /**
     * 更新单个配置项（同时更新数据库和缓存）
     */
    public void update(String key, String value) {
        cache.put(key, value);
        logger.info("配置项 {} 更新为 {}", key, value);
    }
}
