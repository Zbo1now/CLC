package com.campuscoin.dao;

import com.campuscoin.model.EquipmentLoan;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.time.LocalDate;

@Mapper
public interface EquipmentLoanDao {

    @Insert("INSERT INTO equipment_loans (team_id, equipment_id, start_date, end_date, status, held_cost) " +
            "VALUES (#{teamId}, #{equipmentId}, #{startDate}, #{endDate}, #{status}, #{heldCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(EquipmentLoan loan);

    @Select("SELECT COUNT(1) FROM equipment_loans " +
            "WHERE equipment_id = #{equipmentId} " +
            "AND status IN ('PENDING','BORROWED') " +
            "AND NOT (end_date < #{startDate} OR start_date > #{endDate})")
    int countOverlappingActive(@Param("equipmentId") int equipmentId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, start_date AS startDate, end_date AS endDate, status, held_cost AS heldCost, " +
            "borrowed_at AS borrowedAt, returned_at AS returnedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE id = #{id}")
    EquipmentLoan findById(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, start_date AS startDate, end_date AS endDate, status, held_cost AS heldCost, " +
            "borrowed_at AS borrowedAt, returned_at AS returnedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE id = #{id} FOR UPDATE")
    EquipmentLoan findByIdForUpdate(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, start_date AS startDate, end_date AS endDate, status, held_cost AS heldCost, " +
            "borrowed_at AS borrowedAt, returned_at AS returnedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans " +
            "WHERE team_id = #{teamId} AND status IN ('BORROWED','PENDING') " +
            "ORDER BY (CASE WHEN status='BORROWED' THEN 0 ELSE 1 END), created_at DESC LIMIT 1")
    EquipmentLoan findMyActive(@Param("teamId") int teamId);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, start_date AS startDate, end_date AS endDate, status, held_cost AS heldCost, " +
            "borrowed_at AS borrowedAt, returned_at AS returnedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans " +
            "WHERE equipment_id = #{equipmentId} AND status IN ('PENDING','BORROWED') " +
            "AND NOT (end_date < #{startDate} OR start_date > #{endDate}) " +
            "ORDER BY start_date ASC")
    List<EquipmentLoan> listOverlappingActive(@Param("equipmentId") int equipmentId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, start_date AS startDate, end_date AS endDate, status, held_cost AS heldCost, " +
            "borrowed_at AS borrowedAt, returned_at AS returnedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE status = #{status} ORDER BY created_at ASC")
    List<EquipmentLoan> adminListByStatus(@Param("status") String status);

    @Update("UPDATE equipment_loans SET status='BORROWED', borrowed_at=NOW() WHERE id=#{id} AND status='PENDING'")
    int markBorrowed(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='RETURNED', returned_at=NOW() WHERE id=#{id} AND status='BORROWED'")
    int markReturned(@Param("id") int id);
}
