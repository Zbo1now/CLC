package com.campuscoin.dao;

import com.campuscoin.model.Equipment;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EquipmentDao {

    @Select("SELECT id, equipment_name AS equipmentName, model, equipment_type AS equipmentType, rate_per_day AS ratePerDay, " +
            "quantity, available_quantity AS availableQuantity, status, created_at AS createdAt " +
            "FROM equipments ORDER BY id ASC")
    List<Equipment> listAll();

    @Select("SELECT id, equipment_name AS equipmentName, model, equipment_type AS equipmentType, rate_per_day AS ratePerDay, " +
            "quantity, available_quantity AS availableQuantity, status, created_at AS createdAt " +
            "FROM equipments WHERE id = #{id}")
    Equipment findById(@Param("id") int id);

    @Select("SELECT id, equipment_name AS equipmentName, model, equipment_type AS equipmentType, rate_per_day AS ratePerDay, " +
            "quantity, available_quantity AS availableQuantity, status, created_at AS createdAt " +
            "FROM equipments WHERE id = #{id} FOR UPDATE")
    Equipment findByIdForUpdate(@Param("id") int id);

    /**
     * 可用库存增量更新（用于借出/归还）
     * 注意：available_quantity 仅表示“当前未借出数量”，不代表未来日期占用。
     */
    @Update("UPDATE equipments SET available_quantity = available_quantity + #{delta} " +
            "WHERE id = #{id} AND available_quantity + #{delta} >= 0")
    int updateAvailableQuantityDelta(@Param("id") int id, @Param("delta") int delta);

    // ========== 资源管理新增方法 ==========

    @Select("<script>" +
            "SELECT id, equipment_name AS equipmentName, model, equipment_type AS equipmentType, " +
            "rate_per_day AS ratePerDay, quantity, available_quantity AS availableQuantity, status, created_at AS createdAt " +
            "FROM equipments " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (equipment_name LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%') " +
            "OR equipment_type LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Equipment> listPaged(@Param("status") String status, @Param("keyword") String keyword, 
                              @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM equipments " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (equipment_name LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%') " +
            "OR equipment_type LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "</script>")
    int countByCondition(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM equipments")
    int countAll();

    @Select("SELECT COUNT(*) FROM equipments WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Insert("INSERT INTO equipments (equipment_name, model, equipment_type, rate_per_day, quantity, available_quantity, status, created_at) " +
            "VALUES (#{equipmentName}, #{model}, #{equipmentType}, #{ratePerDay}, #{quantity}, #{quantity}, 'AVAILABLE', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(@Param("equipmentName") String equipmentName, @Param("model") String model,
               @Param("equipmentType") String equipmentType, @Param("ratePerDay") Integer ratePerDay,
               @Param("quantity") Integer quantity);

    @Update("<script>" +
            "UPDATE equipments " +
            "<set>" +
            "<if test='params.equipmentName != null'>equipment_name = #{params.equipmentName},</if>" +
            "<if test='params.model != null'>model = #{params.model},</if>" +
            "<if test='params.equipmentType != null'>equipment_type = #{params.equipmentType},</if>" +
            "<if test='params.ratePerDay != null'>rate_per_day = #{params.ratePerDay},</if>" +
            "<if test='params.quantity != null'>quantity = #{params.quantity},</if>" +
            "<if test='params.availableQuantity != null'>available_quantity = #{params.availableQuantity},</if>" +
            "<if test='params.status != null'>status = #{params.status},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(@Param("id") Integer id, @Param("params") Map<String, Object> params);

    @Delete("DELETE FROM equipments WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    @Update("UPDATE equipments SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
}
