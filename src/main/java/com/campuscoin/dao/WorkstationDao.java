package com.campuscoin.dao;

import com.campuscoin.model.Workstation;
import org.apache.ibatis.annotations.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkstationDao {

    @Select("SELECT id, station_code AS stationCode, location, monthly_rent AS monthlyRent, created_at AS createdAt " +
            "FROM workstations ORDER BY station_code")
    List<Workstation> listAll();

    // 带当前月占用状态（自然月）：occupied=1 表示当月已被租用
    @Select("SELECT w.id, w.station_code AS stationCode, w.location, w.monthly_rent AS monthlyRent, w.created_at AS createdAt, " +
            "CASE WHEN EXISTS (" +
            "  SELECT 1 FROM workstation_leases l " +
            "  WHERE l.workstation_id = w.id AND l.status = 'ACTIVE' " +
            "    AND l.start_month <= #{currentMonthStart} AND l.end_month >= #{currentMonthStart}" +
            ") THEN 1 ELSE 0 END AS occupied " +
            "FROM workstations w ORDER BY w.station_code")
    List<Workstation> listAllWithCurrentMonthStatus(@Param("currentMonthStart") Date currentMonthStart);

    @Select("SELECT id, station_code AS stationCode, location, monthly_rent AS monthlyRent, created_at AS createdAt " +
            "FROM workstations WHERE id = #{id}")
    Workstation findById(@Param("id") int id);

    // 租用/续租的并发保护：锁住工位行
    @Select("SELECT id, station_code AS stationCode, location, monthly_rent AS monthlyRent, created_at AS createdAt " +
            "FROM workstations WHERE id = #{id} FOR UPDATE")
    Workstation findByIdForUpdate(@Param("id") int id);

    // ========== 资源管理新增方法 ==========

    @Select("<script>" +
            "SELECT id, station_code AS stationCode, area, location, monthly_rent AS monthlyRent, status, created_at AS createdAt " +
            "FROM workstations " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (station_code LIKE CONCAT('%', #{keyword}, '%') OR location LIKE CONCAT('%', #{keyword}, '%') OR area LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "ORDER BY id DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Workstation> listPaged(@Param("status") String status, @Param("keyword") String keyword, 
                                 @Param("offset") int offset, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM workstations " +
            "<where>" +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (station_code LIKE CONCAT('%', #{keyword}, '%') OR location LIKE CONCAT('%', #{keyword}, '%') OR area LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "</script>")
    int countByCondition(@Param("status") String status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM workstations")
    int countAll();

    @Select("SELECT COUNT(*) FROM workstations WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    @Insert("INSERT INTO workstations (station_code, area, location, monthly_rent, status, created_at) " +
            "VALUES (#{stationCode}, #{area}, #{location}, #{monthlyRent}, 'AVAILABLE', NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(@Param("stationCode") String stationCode, @Param("area") String area, 
               @Param("location") String location, @Param("monthlyRent") Integer monthlyRent);

    @Update("<script>" +
            "UPDATE workstations " +
            "<set>" +
            "<if test='params.stationCode != null'>station_code = #{params.stationCode},</if>" +
            "<if test='params.area != null'>area = #{params.area},</if>" +
            "<if test='params.location != null'>location = #{params.location},</if>" +
            "<if test='params.monthlyRent != null'>monthly_rent = #{params.monthlyRent},</if>" +
            "<if test='params.status != null'>status = #{params.status},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(@Param("id") Integer id, @Param("params") Map<String, Object> params);

    @Delete("DELETE FROM workstations WHERE id = #{id}")
    int delete(@Param("id") Integer id);

    @Update("UPDATE workstations SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
}
