package com.campuscoin.dao;

import com.campuscoin.model.WorkstationLease;
import org.apache.ibatis.annotations.*;

import java.sql.Date;

@Mapper
public interface WorkstationLeaseDao {

    @Select("SELECT COUNT(*) FROM workstation_leases " +
            "WHERE workstation_id = #{workstationId} AND status = 'ACTIVE' " +
            "AND NOT (end_month < #{startMonth} OR start_month > #{endMonth})")
    int countOverlappingActive(@Param("workstationId") int workstationId,
                              @Param("startMonth") Date startMonth,
                              @Param("endMonth") Date endMonth);

    @Select("SELECT COUNT(*) FROM workstation_leases " +
            "WHERE workstation_id = #{workstationId} AND status = 'ACTIVE' AND id <> #{excludeId} " +
            "AND NOT (end_month < #{startMonth} OR start_month > #{endMonth})")
    int countOverlappingActiveExclude(@Param("workstationId") int workstationId,
                                     @Param("excludeId") int excludeId,
                                     @Param("startMonth") Date startMonth,
                                     @Param("endMonth") Date endMonth);

    @Insert("INSERT INTO workstation_leases (workstation_id, team_id, start_month, end_month, monthly_rent, total_cost, status) " +
            "VALUES (#{workstationId}, #{teamId}, #{startMonth}, #{endMonth}, #{monthlyRent}, #{totalCost}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(WorkstationLease lease);

    // 查询当前团队在当前月有效的租约（ACTIVE 且 end_month >= 当月月初）
    @Select("SELECT id, workstation_id AS workstationId, team_id AS teamId, start_month AS startMonth, end_month AS endMonth, " +
            "monthly_rent AS monthlyRent, total_cost AS totalCost, status, released_at AS releasedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM workstation_leases " +
            "WHERE team_id = #{teamId} AND status = 'ACTIVE' AND end_month >= #{currentMonthStart} " +
            "ORDER BY end_month DESC LIMIT 1")
    WorkstationLease findActiveByTeam(@Param("teamId") int teamId,
                                     @Param("currentMonthStart") Date currentMonthStart);

    // 查询团队最近一条 ACTIVE 租约（可能已到期），用于“到期后续租/释放”场景
    @Select("SELECT id, workstation_id AS workstationId, team_id AS teamId, start_month AS startMonth, end_month AS endMonth, " +
            "monthly_rent AS monthlyRent, total_cost AS totalCost, status, released_at AS releasedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM workstation_leases " +
            "WHERE team_id = #{teamId} AND status = 'ACTIVE' " +
            "ORDER BY end_month DESC LIMIT 1")
    WorkstationLease findLatestActiveByTeam(@Param("teamId") int teamId);

    // 锁住租约行，用于续租/释放
    @Select("SELECT id, workstation_id AS workstationId, team_id AS teamId, start_month AS startMonth, end_month AS endMonth, " +
            "monthly_rent AS monthlyRent, total_cost AS totalCost, status, released_at AS releasedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM workstation_leases WHERE id = #{id} FOR UPDATE")
    WorkstationLease findByIdForUpdate(@Param("id") int id);

    @Update("UPDATE workstation_leases SET end_month = #{endMonth}, total_cost = #{totalCost} WHERE id = #{id}")
    int updateEndMonthAndCost(@Param("id") int id,
                              @Param("endMonth") Date endMonth,
                              @Param("totalCost") int totalCost);

    @Update("UPDATE workstation_leases SET status = 'RELEASED', released_at = NOW() WHERE id = #{id}")
    int release(@Param("id") int id);
}
