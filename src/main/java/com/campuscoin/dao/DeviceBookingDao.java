        // ...existing code...
package com.campuscoin.dao;

import com.campuscoin.model.DeviceBooking;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeviceBookingDao {

    @Select("SELECT COUNT(*) FROM device_bookings " +
            "WHERE device_id = #{deviceId} AND status IN ('RESERVED','IN_USE') " +
            "AND NOT (end_at <= #{startAt} OR start_at >= #{endAt})")
    int countOverlappingActive(@Param("deviceId") int deviceId,
                              @Param("startAt") LocalDateTime startAt,
                              @Param("endAt") LocalDateTime endAt);

    @Insert("INSERT INTO device_bookings (device_id, team_id, start_at, end_at, status, billed_cost) " +
            "VALUES (#{deviceId}, #{teamId}, #{startAt}, #{endAt}, #{status}, #{billedCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(DeviceBooking booking);

    @Select("SELECT id, device_id AS deviceId, team_id AS teamId, start_at AS startAt, end_at AS endAt, " +
            "actual_start_at AS actualStartAt, actual_end_at AS actualEndAt, status, billed_cost AS billedCost, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM device_bookings " +
            "WHERE team_id = #{teamId} AND status IN ('RESERVED','IN_USE') AND end_at >= #{now} " +
            "ORDER BY (CASE WHEN status = 'IN_USE' THEN 0 ELSE 1 END), start_at ASC LIMIT 1")
    DeviceBooking findMyActive(@Param("teamId") int teamId,
                              @Param("now") LocalDateTime now);

    @Select("SELECT id, device_id AS deviceId, team_id AS teamId, start_at AS startAt, end_at AS endAt, " +
            "actual_start_at AS actualStartAt, actual_end_at AS actualEndAt, status, billed_cost AS billedCost, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM device_bookings WHERE id = #{id}")
    DeviceBooking findById(@Param("id") int id);

    @Select("SELECT id, device_id AS deviceId, team_id AS teamId, start_at AS startAt, end_at AS endAt, " +
            "actual_start_at AS actualStartAt, actual_end_at AS actualEndAt, status, billed_cost AS billedCost, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM device_bookings WHERE id = #{id} FOR UPDATE")
    DeviceBooking findByIdForUpdate(@Param("id") int id);

    @Update("UPDATE device_bookings SET status = 'IN_USE', actual_start_at = NOW() " +
            "WHERE id = #{id} AND status = 'RESERVED'")
    int start(@Param("id") int id);

    @Update("UPDATE device_bookings SET status = 'FINISHED', actual_end_at = NOW(), billed_cost = #{billedCost} " +
            "WHERE id = #{id} AND status = 'IN_USE'")
    int finish(@Param("id") int id, @Param("billedCost") int billedCost);

    @Update("UPDATE device_bookings SET status = 'FINISHED', actual_end_at = #{actualEndAt}, billed_cost = #{billedCost} " +
            "WHERE id = #{id} AND status = 'IN_USE'")
    int finishAt(@Param("id") int id,
                 @Param("billedCost") int billedCost,
                 @Param("actualEndAt") LocalDateTime actualEndAt);

    @Select("SELECT id FROM device_bookings WHERE status = 'IN_USE' AND end_at <= #{now} ORDER BY end_at ASC LIMIT #{limit}")
    List<Integer> listDueFinishIds(@Param("now") LocalDateTime now, @Param("limit") int limit);
    
    /**
     * 批量将到点的RESERVED预约转为IN_USE，actual_start_at设为start_at
     */
    @Update("UPDATE device_bookings SET status = 'IN_USE', actual_start_at = start_at WHERE status = 'RESERVED' AND start_at <= #{now}")
    int autoStart(@Param("now") java.time.LocalDateTime now);

    /**
     * 批量将到点的IN_USE预约转为FINISHED，actual_end_at设为end_at
     */
    @Update("UPDATE device_bookings SET status = 'FINISHED', actual_end_at = end_at WHERE status = 'IN_USE' AND end_at <= #{now}")
    int autoFinish(@Param("now") java.time.LocalDateTime now);
}
