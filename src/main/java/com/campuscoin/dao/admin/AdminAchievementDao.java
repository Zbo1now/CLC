package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminAchievementListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 成果审核 DAO（管理员后台）
 */
@Mapper
public interface AdminAchievementDao {

    /**
     * 分页查询成果列表（带筛选条件）
     * @param status 状态筛选：null=全部，PENDING=待审核，APPROVED=已通过，REJECTED=已驳回
     * @param category 类型筛选
     * @param teamName 团队名称模糊搜索
     * @param offset 分页偏移量
     * @param pageSize 每页数量
     * @return 成果列表
     */
    @Select({
            "<script>",
            "SELECT ",
            "  a.id, ",
            "  a.team_id AS teamId, ",
            "  t.team_name AS teamName, ",
            "  a.category, ",
            "  a.sub_type AS subType, ",
            "  a.title, ",
            "  a.proof_url AS proofUrl, ",
            "  a.description, ",
            "  a.status, ",
            "  a.reward_coins AS rewardCoins, ",
            "  a.reject_reason AS rejectReason, ",
            "  a.reviewed_by AS reviewedBy, ",
            "  a.reviewed_at AS reviewedAt, ",
            "  a.created_at AS createdAt ",
            "FROM achievement_submissions a ",
            "JOIN teams t ON a.team_id = t.id ",
            "<where>",
            "  <if test='status != null and status != \"\"'>",
            "    AND a.status = #{status}",
            "  </if>",
            "  <if test='category != null and category != \"\"'>",
            "    AND a.category = #{category}",
            "  </if>",
            "  <if test='teamName != null and teamName != \"\"'>",
            "    AND t.team_name LIKE CONCAT('%', #{teamName}, '%')",
            "  </if>",
            "</where>",
            "ORDER BY a.created_at DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<AdminAchievementListItem> listPaged(@Param("status") String status,
                                             @Param("category") String category,
                                             @Param("teamName") String teamName,
                                             @Param("offset") int offset,
                                             @Param("pageSize") int pageSize);

    /**
     * 统计成果总数（带筛选条件）
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) ",
            "FROM achievement_submissions a ",
            "JOIN teams t ON a.team_id = t.id ",
            "<where>",
            "  <if test='status != null and status != \"\"'>",
            "    AND a.status = #{status}",
            "  </if>",
            "  <if test='category != null and category != \"\"'>",
            "    AND a.category = #{category}",
            "  </if>",
            "  <if test='teamName != null and teamName != \"\"'>",
            "    AND t.team_name LIKE CONCAT('%', #{teamName}, '%')",
            "  </if>",
            "</where>",
            "</script>"
    })
    int countByCondition(@Param("status") String status,
                        @Param("category") String category,
                        @Param("teamName") String teamName);

    /**
     * 获取成果详情（带团队名称）
     */
    @Select("SELECT " +
            "  a.id, " +
            "  a.team_id AS teamId, " +
            "  t.team_name AS teamName, " +
            "  a.category, " +
            "  a.sub_type AS subType, " +
            "  a.title, " +
            "  a.proof_url AS proofUrl, " +
            "  a.description, " +
            "  a.status, " +
            "  a.reward_coins AS rewardCoins, " +
            "  a.reject_reason AS rejectReason, " +
            "  a.reviewed_by AS reviewedBy, " +
            "  a.reviewed_at AS reviewedAt, " +
            "  a.created_at AS createdAt " +
            "FROM achievement_submissions a " +
            "JOIN teams t ON a.team_id = t.id " +
            "WHERE a.id = #{id}")
    AdminAchievementListItem getDetailById(@Param("id") Integer id);
}
