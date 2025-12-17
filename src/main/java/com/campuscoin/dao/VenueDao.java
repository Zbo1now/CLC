package com.campuscoin.dao;

import com.campuscoin.model.Venue;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface VenueDao {

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues ORDER BY id ASC")
    List<Venue> listAll();

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues WHERE id = #{id}")
    Venue findById(@Param("id") int id);

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues WHERE id = #{id} FOR UPDATE")
    Venue findByIdForUpdate(@Param("id") int id);

    // ========== 资源管理新增方法 ==========

    @Select("<script>" +
            "SELECT id, venue_name AS venueName, venue_type AS venueType, capacity, " +
            "rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM venues " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (venue_name LIKE CONCAT('%', #{keyword}, '%') OR venue_type LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Venue> listPaged(@Param("status") String status, @Param("keyword") String keyword, 
                          @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM venues " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (venue_name LIKE CONCAT('%', #{keyword}, '%') OR venue_type LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "</script>")
    int countByCondition(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM venues")
    int countAll();

    @Select("SELECT COUNT(*) FROM venues WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Insert("INSERT INTO venues (venue_name, venue_type, capacity, rate_per_hour, status, created_at, updated_at) " +
            "VALUES (#{venueName}, #{venueType}, #{capacity}, #{ratePerHour}, 'AVAILABLE', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(@Param("venueName") String venueName, @Param("venueType") String venueType,
               @Param("capacity") Integer capacity, @Param("ratePerHour") Integer ratePerHour);

    @Update("<script>" +
            "UPDATE venues " +
            "<set>" +
            "<if test='params.venueName != null'>venue_name = #{params.venueName},</if>" +
            "<if test='params.venueType != null'>venue_type = #{params.venueType},</if>" +
            "<if test='params.capacity != null'>capacity = #{params.capacity},</if>" +
            "<if test='params.ratePerHour != null'>rate_per_hour = #{params.ratePerHour},</if>" +
            "<if test='params.status != null'>status = #{params.status},</if>" +
            "updated_at = NOW()" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(@Param("id") Integer id, @Param("params") Map<String, Object> params);

    @Delete("DELETE FROM venues WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    @Update("UPDATE venues SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
}
