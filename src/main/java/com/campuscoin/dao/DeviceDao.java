package com.campuscoin.dao;

import com.campuscoin.model.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeviceDao {

    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt, " +
            "CASE " +
            "  WHEN d.status = 'MAINTENANCE' THEN 'MAINTENANCE' " +
            "  WHEN EXISTS (SELECT 1 FROM device_bookings b WHERE b.device_id = d.id AND b.status = 'IN_USE') THEN 'IN_USE' " +
            "  ELSE 'IDLE' " +
            "END AS runtimeStatus " +
            "FROM devices d ORDER BY d.device_name")
    List<Device> listAllWithRuntimeStatus();

    // 带指定时间段可用性（RESERVED/IN_USE 任一冲突即不可用）
    @Select("SELECT d.id, d.device_name AS deviceName, d.device_type AS deviceType, d.rate_per_hour AS ratePerHour, d.status, d.created_at AS createdAt, " +
            "CASE " +
            "  WHEN d.status = 'MAINTENANCE' THEN 'MAINTENANCE' " +
            "  WHEN EXISTS (SELECT 1 FROM device_bookings b WHERE b.device_id = d.id AND b.status = 'IN_USE') THEN 'IN_USE' " +
            "  ELSE 'IDLE' " +
            "END AS runtimeStatus, " +
            "CASE " +
            "  WHEN d.status = 'MAINTENANCE' THEN 0 " +
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
}
