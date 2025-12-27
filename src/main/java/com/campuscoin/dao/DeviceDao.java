package com.campuscoin.dao;

import com.campuscoin.model.Device;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceDao {

    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt, " +
            "CASE " +
            "  WHEN d.status IN ('MAINTENANCE','BROKEN') THEN 'MAINTENANCE' " +
            "  WHEN EXISTS (SELECT 1 FROM device_bookings b WHERE b.device_id = d.id AND b.status = 'IN_USE') THEN 'IN_USE' " +
            "  ELSE 'IDLE' " +
            "END AS runtimeStatus " +
            "FROM devices d ORDER BY d.device_name")
    List<Device> listAllWithRuntimeStatus();

    // 带指定时间段可用性（RESERVED/IN_USE 任一冲突即不可用）
    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt, " +
            "CASE " +
            "  WHEN d.status IN ('MAINTENANCE','BROKEN') THEN 'MAINTENANCE' " +
            "  WHEN EXISTS (SELECT 1 FROM device_bookings b WHERE b.device_id = d.id AND b.status = 'IN_USE') THEN 'IN_USE' " +
            "  ELSE 'IDLE' " +
            "END AS runtimeStatus, " +
            "CASE " +
            "  WHEN d.status IN ('MAINTENANCE','BROKEN','IN_USE') THEN 0 " +
            "  WHEN EXISTS (" +
            "    SELECT 1 FROM device_bookings b2 " +
            "    WHERE b2.device_id = d.id AND b2.status IN ('RESERVED','IN_USE') " +
            "      AND NOT (b2.end_at <= #{startAt} OR b2.start_at >= #{endAt})" +
            "  ) THEN 0 " +
            "  ELSE 1 " +
            "END AS available " +
            "FROM devices d ORDER BY d.device_name")
    List<Device> listAllWithAvailability(@Param("startAt") LocalDateTime startAt,
                                        @Param("endAt") LocalDateTime endAt);

    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt " +
            "FROM devices d WHERE d.id = #{id}")
    Device findById(@Param("id") int id);

    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt " +
            "FROM devices d WHERE d.id = #{id} FOR UPDATE")
    Device findByIdForUpdate(@Param("id") int id);

    // ========== 资源管理新增方法 ==========

    @Select("<script>" +
            "SELECT id, device_name AS deviceName, model, device_type AS deviceType, location, " +
            "rate_per_hour AS ratePerHour, status, created_at AS createdAt " +
            "FROM devices " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (device_name LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%') " +
            "OR device_type LIKE CONCAT('%', #{keyword}, '%') OR location LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Device> listPaged(@Param("status") String status, @Param("keyword") String keyword, 
                           @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM devices " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (device_name LIKE CONCAT('%', #{keyword}, '%') OR model LIKE CONCAT('%', #{keyword}, '%') " +
            "OR device_type LIKE CONCAT('%', #{keyword}, '%') OR location LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "</script>")
    int countByCondition(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM devices")
    int countAll();

    @Select("SELECT COUNT(*) FROM devices WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Insert("INSERT INTO devices (device_name, model, device_type, location, rate_per_hour, status, created_at) " +
            "VALUES (#{deviceName}, #{model}, #{deviceType}, #{location}, #{ratePerHour}, 'AVAILABLE', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(@Param("deviceName") String deviceName, @Param("model") String model,
               @Param("deviceType") String deviceType, @Param("location") String location,
               @Param("ratePerHour") Integer ratePerHour);

    @Update("<script>" +
            "UPDATE devices " +
            "<set>" +
            "<if test='params.deviceName != null'>device_name = #{params.deviceName},</if>" +
            "<if test='params.model != null'>model = #{params.model},</if>" +
            "<if test='params.deviceType != null'>device_type = #{params.deviceType},</if>" +
            "<if test='params.location != null'>location = #{params.location},</if>" +
            "<if test='params.ratePerHour != null'>rate_per_hour = #{params.ratePerHour},</if>" +
            "<if test='params.status != null'>status = #{params.status},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(@Param("id") Integer id, @Param("params") Map<String, Object> params);

    @Delete("DELETE FROM devices WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    @Update("UPDATE devices SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
}
