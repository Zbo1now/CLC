package com.campuscoin.dao;

import com.campuscoin.model.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 系统配置 DAO
 */
@Mapper
public interface SystemConfigDao {

    /**
     * 查询所有配置（按分类和排序）
     */
    @Select("SELECT id, config_key, config_value, value_type, " +
            "category, display_name, description, unit, min_value, " +
            "max_value, sort_order, is_enabled, " +
            "updated_by, updated_at, created_at " +
            "FROM system_configs WHERE is_enabled = 1 " +
            "ORDER BY FIELD(category, 'REWARD', 'ACTIVITY', 'RESOURCE', 'SYSTEM'), sort_order ASC")
    List<SystemConfig> listAll();

    /**
     * 根据分类查询配置
     */
    @Select("SELECT id, config_key, config_value, value_type, " +
            "category, display_name, description, unit, min_value, " +
            "max_value, sort_order, is_enabled, " +
            "updated_by, updated_at, created_at " +
            "FROM system_configs WHERE category = #{category} AND is_enabled = 1 " +
            "ORDER BY sort_order ASC")
    List<SystemConfig> listByCategory(@Param("category") String category);

    /**
     * 根据配置键查询
     */
    @Select("SELECT id, config_key, config_value, value_type, " +
            "category, display_name, description, unit, min_value, " +
            "max_value, sort_order, is_enabled, " +
            "updated_by, updated_at, created_at " +
            "FROM system_configs WHERE config_key = #{configKey}")
    SystemConfig findByKey(@Param("configKey") String configKey);

    /**
     * 根据ID查询
     */
    @Select("SELECT id, config_key, config_value, value_type, " +
            "category, display_name, description, unit, min_value, " +
            "max_value, sort_order, is_enabled, " +
            "updated_by, updated_at, created_at " +
            "FROM system_configs WHERE id = #{id}")
    SystemConfig findById(@Param("id") Integer id);

    /**
     * 更新配置值
     */
    @Update("UPDATE system_configs SET config_value = #{configValue}, updated_by = #{updatedBy} " +
            "WHERE id = #{id}")
    int updateValue(@Param("id") Integer id, 
                   @Param("configValue") String configValue,
                   @Param("updatedBy") String updatedBy);

    /**
     * 批量更新配置值
     */
    @Update("<script>" +
            "<foreach collection='configs' item='item' separator=';'>" +
            "UPDATE system_configs SET config_value = #{item.configValue}, updated_by = #{updatedBy} " +
            "WHERE id = #{item.id}" +
            "</foreach>" +
            "</script>")
    void batchUpdate(@Param("configs") List<SystemConfig> configs, 
                    @Param("updatedBy") String updatedBy);

    /**
     * 创建新配置
     */
    @Insert("INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, " +
            "description, unit, min_value, max_value, sort_order, updated_by) " +
            "VALUES (#{configKey}, #{configValue}, #{valueType}, #{category}, #{displayName}, " +
            "#{description}, #{unit}, #{minValue}, #{maxValue}, #{sortOrder}, #{updatedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(SystemConfig config);

    /**
     * 删除配置（逻辑删除，设置为禁用）
     */
    @Update("UPDATE system_configs SET is_enabled = 0, updated_by = #{updatedBy} WHERE id = #{id}")
    int disable(@Param("id") Integer id, @Param("updatedBy") String updatedBy);

    /**
     * 物理删除配置
     */
    @Delete("DELETE FROM system_configs WHERE id = #{id}")
    int delete(@Param("id") Integer id);
}
