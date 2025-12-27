package com.campuscoin.dao;

import com.campuscoin.model.EquipmentLoan;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.time.LocalDate;

@Mapper
public interface EquipmentLoanDao {

    @Insert("INSERT INTO equipment_loans (team_id, equipment_id, quantity, start_date, end_date, status, held_cost, final_cost) " +
            "VALUES (#{teamId}, #{equipmentId}, #{quantity}, #{startDate}, #{endDate}, #{status}, #{heldCost}, #{finalCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(EquipmentLoan loan);

    @Select("SELECT COUNT(1) FROM equipment_loans " +
            "WHERE equipment_id = #{equipmentId} " +
            "AND status IN ('PENDING','APPROVED','BORROWED') " +
            "AND NOT (end_date < #{startDate} OR start_date > #{endDate})")
    int countOverlappingActive(@Param("equipmentId") int equipmentId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, quantity, start_date AS startDate, end_date AS endDate, status, " +
            "held_cost AS heldCost, final_cost AS finalCost, approved_at AS approvedAt, rejected_at AS rejectedAt, " +
            "cancelled_at AS cancelledAt, expired_at AS expiredAt, borrowed_at AS borrowedAt, returned_at AS returnedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE id = #{id}")
    EquipmentLoan findById(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, quantity, start_date AS startDate, end_date AS endDate, status, " +
            "held_cost AS heldCost, final_cost AS finalCost, approved_at AS approvedAt, rejected_at AS rejectedAt, " +
            "cancelled_at AS cancelledAt, expired_at AS expiredAt, borrowed_at AS borrowedAt, returned_at AS returnedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE id = #{id} FOR UPDATE")
    EquipmentLoan findByIdForUpdate(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, quantity, start_date AS startDate, end_date AS endDate, status, " +
            "held_cost AS heldCost, final_cost AS finalCost, approved_at AS approvedAt, rejected_at AS rejectedAt, " +
            "cancelled_at AS cancelledAt, expired_at AS expiredAt, borrowed_at AS borrowedAt, returned_at AS returnedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans " +
            "WHERE team_id = #{teamId} AND status IN ('BORROWED','APPROVED','PENDING') " +
            "ORDER BY (CASE WHEN status='BORROWED' THEN 0 WHEN status='APPROVED' THEN 1 ELSE 2 END), created_at DESC LIMIT 1")
    EquipmentLoan findMyActive(@Param("teamId") int teamId);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, quantity, start_date AS startDate, end_date AS endDate, status, " +
            "held_cost AS heldCost, final_cost AS finalCost, approved_at AS approvedAt, rejected_at AS rejectedAt, " +
            "cancelled_at AS cancelledAt, expired_at AS expiredAt, borrowed_at AS borrowedAt, returned_at AS returnedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans " +
            "WHERE equipment_id = #{equipmentId} AND status IN ('PENDING','APPROVED','BORROWED') " +
            "AND NOT (end_date < #{startDate} OR start_date > #{endDate}) " +
            "ORDER BY start_date ASC")
    List<EquipmentLoan> listOverlappingActive(@Param("equipmentId") int equipmentId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    /**
     * 统计日期范围内已被占用的数量（PENDING/APPROVED/BORROWED）
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM equipment_loans " +
            "WHERE equipment_id = #{equipmentId} " +
            "AND status IN ('PENDING','APPROVED','BORROWED') " +
            "AND NOT (end_date < #{startDate} OR start_date > #{endDate})")
    int sumOverlappingActiveQuantity(@Param("equipmentId") int equipmentId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Select("SELECT id, team_id AS teamId, equipment_id AS equipmentId, quantity, start_date AS startDate, end_date AS endDate, status, " +
            "held_cost AS heldCost, final_cost AS finalCost, approved_at AS approvedAt, rejected_at AS rejectedAt, " +
            "cancelled_at AS cancelledAt, expired_at AS expiredAt, borrowed_at AS borrowedAt, returned_at AS returnedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM equipment_loans WHERE status = #{status} ORDER BY created_at ASC")
    List<EquipmentLoan> adminListByStatus(@Param("status") String status);

    @Update("UPDATE equipment_loans SET status='APPROVED', approved_at=NOW() WHERE id=#{id} AND status='PENDING'")
    int markApproved(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='REJECTED', rejected_at=NOW() WHERE id=#{id} AND status IN ('PENDING','APPROVED')")
    int markRejected(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='CANCELLED', cancelled_at=NOW() WHERE id=#{id} AND status IN ('PENDING','APPROVED')")
    int markCancelled(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='EXPIRED', expired_at=NOW() WHERE id=#{id} AND status='PENDING'")
    int markExpired(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='BORROWED', borrowed_at=NOW() WHERE id=#{id} AND status IN ('PENDING','APPROVED')")
    int markBorrowed(@Param("id") int id);

    @Update("UPDATE equipment_loans SET status='RETURNED', returned_at=NOW(), final_cost=#{finalCost} WHERE id=#{id} AND status='BORROWED'")
    int markReturned(@Param("id") int id, @Param("finalCost") int finalCost);

    @Select("SELECT id FROM equipment_loans WHERE status='PENDING' AND created_at <= #{deadline} ORDER BY created_at ASC LIMIT #{limit}")
    List<Integer> listExpiredCandidateIds(@Param("deadline") java.sql.Timestamp deadline, @Param("limit") int limit);
}
