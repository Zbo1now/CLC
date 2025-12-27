package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminEquipmentLoanView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 后台管理 - 器材借用查询（带团队/器材名称）
 */
@Mapper
public interface AdminEquipmentLoanDao {

    @Select({
            "<script>",
            "SELECT",
            "  l.id,",
            "  l.team_id AS teamId,",
            "  t.team_name AS teamName,",
            "  l.equipment_id AS equipmentId,",
            "  e.equipment_name AS equipmentName,",
            "  l.quantity,",
            "  l.start_date AS startDate,",
            "  l.end_date AS endDate,",
            "  l.status,",
            "  l.held_cost AS heldCost,",
            "  l.final_cost AS finalCost,",
            "  l.approved_at AS approvedAt,",
            "  l.rejected_at AS rejectedAt,",
            "  l.cancelled_at AS cancelledAt,",
            "  l.expired_at AS expiredAt,",
            "  l.borrowed_at AS borrowedAt,",
            "  l.returned_at AS returnedAt,",
            "  l.created_at AS createdAt,",
            "  l.updated_at AS updatedAt",
            "FROM equipment_loans l",
            "LEFT JOIN teams t ON t.id = l.team_id",
            "LEFT JOIN equipments e ON e.id = l.equipment_id",
            "WHERE 1=1",
            "<if test='status != null and status != \"\"'>",
            "  AND l.status = #{status}",
            "</if>",
            "ORDER BY l.created_at DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AdminEquipmentLoanView> listPaged(@Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM equipment_loans l",
            "WHERE 1=1",
            "<if test='status != null and status != \"\"'>",
            "  AND l.status = #{status}",
            "</if>",
            "</script>"
    })
    int count(@Param("status") String status);
}


