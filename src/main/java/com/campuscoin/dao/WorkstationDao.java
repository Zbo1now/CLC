package com.campuscoin.dao;

import com.campuscoin.model.Workstation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Date;
import java.util.List;

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
}
