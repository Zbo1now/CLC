package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminTeamListItem;
import com.campuscoin.model.admin.AdminTeamDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 团队管理 DAO
 * 为后台管理系统提供团队查询和统计功能
 */
@Mapper
public interface AdminTeamDao {

    /**
     * 分页查询团队列表（带统计数据）
     * @param keyword 关键词（团队名称或联系人）
     * @param orderBy 排序字段：balance, weeklyCheckins, totalAchievements, createdAt
     * @param orderDir 排序方向：ASC, DESC
     * @param offset 分页偏移量
     * @param pageSize 每页数量
     * @return 团队列表
     */
    @Select({
            "<script>",
            "SELECT ",
            "  t.id, ",
            "  t.team_name AS teamName, ",
            "  t.contact_name AS contactName, ",
            "  t.contact_phone AS contactPhone, ",
            "  t.balance, ",
            "  t.created_at AS createdAt, ",
            "  IFNULL(wc.cnt, 0) AS weeklyCheckins, ",
            "  IFNULL(ta.cnt, 0) AS totalAchievements, ",
            "  IFNULL(wr.cnt, 0) AS weeklyResourceUsage ",
            "FROM teams t ",
            // 本周打卡次数（从 transactions 表统计 CHECKIN 类型）
            "LEFT JOIN ( ",
            "  SELECT team_id, COUNT(*) AS cnt ",
            "  FROM transactions ",
            "  WHERE txn_type = 'CHECKIN' ",
            "    AND created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "    AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "  GROUP BY team_id ",
            ") wc ON t.id = wc.team_id ",
            // 累计成果数
            "LEFT JOIN ( ",
            "  SELECT team_id, COUNT(*) AS cnt ",
            "  FROM achievement_submissions ",
            "  GROUP BY team_id ",
            ") ta ON t.id = ta.team_id ",
            // 本周资源使用次数（设备+场地+器材+工位）
            "LEFT JOIN ( ",
            "  SELECT team_id, COUNT(*) AS cnt FROM ( ",
            "    SELECT team_id, created_at FROM device_bookings ",
            "    WHERE created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "      AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "    UNION ALL ",
            "    SELECT team_id, created_at FROM venue_bookings ",
            "    WHERE created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "      AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "    UNION ALL ",
            "    SELECT team_id, created_at FROM equipment_loans ",
            "    WHERE created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "      AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "    UNION ALL ",
            "    SELECT team_id, created_at FROM workstation_leases ",
            "    WHERE created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "      AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "  ) AS all_resources ",
            "  GROUP BY team_id ",
            ") wr ON t.id = wr.team_id ",
            "<if test='keyword != null and keyword != \"\"'>",
            "WHERE t.team_name LIKE CONCAT('%', #{keyword}, '%') ",
            "   OR t.contact_name LIKE CONCAT('%', #{keyword}, '%') ",
            "</if>",
            "ORDER BY ",
            "<choose>",
            "  <when test='orderBy == \"balance\"'>t.balance</when>",
            "  <when test='orderBy == \"weeklyCheckins\"'>weeklyCheckins</when>",
            "  <when test='orderBy == \"totalAchievements\"'>totalAchievements</when>",
            "  <when test='orderBy == \"createdAt\"'>t.created_at</when>",
            "  <otherwise>t.id</otherwise>",
            "</choose>",
            " ",
            "<choose>",
            "  <when test='orderDir == \"ASC\"'>ASC</when>",
            "  <otherwise>DESC</otherwise>",
            "</choose>",
            " LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<AdminTeamListItem> listPaged(@Param("keyword") String keyword,
                                      @Param("orderBy") String orderBy,
                                      @Param("orderDir") String orderDir,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    /**
     * 统计团队总数（带搜索条件）
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) FROM teams ",
            "<if test='keyword != null and keyword != \"\"'>",
            "WHERE team_name LIKE CONCAT('%', #{keyword}, '%') ",
            "   OR contact_name LIKE CONCAT('%', #{keyword}, '%') ",
            "</if>",
            "</script>"
    })
    int countByCondition(@Param("keyword") String keyword);

    /**
     * 查询团队详情（基础信息）
     */
    @Select("SELECT id, team_name AS teamName, contact_name AS contactName, contact_phone AS contactPhone, " +
            "face_image AS faceImage, balance, current_streak AS currentStreak, created_at AS createdAt " +
            "FROM teams WHERE id = #{teamId}")
    AdminTeamDetail getTeamBasicInfo(@Param("teamId") Integer teamId);

    /**
     * 统计团队累计成果数
     */
    @Select("SELECT COUNT(*) FROM achievement_submissions WHERE team_id = #{teamId}")
    Integer countTotalAchievements(@Param("teamId") Integer teamId);

    /**
     * 统计团队累计打卡次数
     */
    @Select("SELECT COUNT(*) FROM transactions WHERE team_id = #{teamId} AND txn_type = 'CHECKIN'")
    Integer countTotalCheckins(@Param("teamId") Integer teamId);

    /**
     * 统计团队累计值班次数
     */
    @Select("SELECT COUNT(*) FROM duty_task_signups WHERE team_id = #{teamId}")
    Integer countTotalDutyTasks(@Param("teamId") Integer teamId);

    /**
     * 统计团队累计培训次数
     */
    @Select("SELECT COUNT(*) FROM training_participations WHERE team_id = #{teamId}")
    Integer countTotalTrainings(@Param("teamId") Integer teamId);

    /**
     * 统计团队本周资源使用次数
     */
    @Select({
            "SELECT COUNT(*) FROM (",
            "  SELECT id FROM device_bookings ",
            "  WHERE team_id = #{teamId} ",
            "    AND created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "    AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "  UNION ALL ",
            "  SELECT id FROM venue_bookings ",
            "  WHERE team_id = #{teamId} ",
            "    AND created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "    AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "  UNION ALL ",
            "  SELECT id FROM equipment_loans ",
            "  WHERE team_id = #{teamId} ",
            "    AND created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "    AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            "  UNION ALL ",
            "  SELECT id FROM workstation_leases ",
            "  WHERE team_id = #{teamId} ",
            "    AND created_at &gt;= DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY) ",
            "    AND created_at &lt; DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 7 DAY) ",
            ") AS all_resources"
    })
    Integer countWeeklyResourceUsage(@Param("teamId") Integer teamId);

    /**
     * 计算团队创新力分数（成果数量 × 平均奖励值）
     */
    @Select("SELECT IFNULL(SUM(reward_coins), 0) FROM achievement_submissions " +
            "WHERE team_id = #{teamId} AND status = 'APPROVED'")
    Integer calcInnovationScore(@Param("teamId") Integer teamId);

    /**
     * 查询团队近7天的流水记录（用于行为时间线）
     */
    @Select("SELECT * FROM transactions " +
            "WHERE team_id = #{teamId} " +
            "  AND created_at &gt;= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "ORDER BY created_at DESC " +
            "LIMIT 50")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "teamId", column = "team_id"),
            @Result(property = "txnType", column = "txn_type"),
            @Result(property = "amount", column = "amount"),
            @Result(property = "description", column = "description"),
            @Result(property = "balanceAfter", column = "balance_after"),
            @Result(property = "refId", column = "ref_id"),
            @Result(property = "refType", column = "ref_type"),
            @Result(property = "createdAt", column = "created_at")
    })
    List<Map<String, Object>> getRecentTransactions(@Param("teamId") Integer teamId);

    /**
     * 查询团队近期成果申报记录
     */
    @Select("SELECT id, title, description, reward_coins AS rewardCoins, status, created_at AS createdAt, reviewed_at AS reviewedAt " +
            "FROM achievement_submissions " +
            "WHERE team_id = #{teamId} " +
            "ORDER BY created_at DESC " +
            "LIMIT 20")
    List<Map<String, Object>> getRecentAchievements(@Param("teamId") Integer teamId);

    /**
     * 查询团队近期资源预约记录（设备+场地）
     */
    @Select({
            "SELECT 'device' AS type, d.device_name AS name, b.start_at AS startTime, b.end_at AS endTime, b.status, b.created_at AS createdAt ",
            "FROM device_bookings b JOIN devices d ON b.device_id = d.id ",
            "WHERE b.team_id = #{teamId} ",
            "ORDER BY b.created_at DESC LIMIT 10"
    })
    List<Map<String, Object>> getRecentDeviceBookings(@Param("teamId") Integer teamId);

    @Select({
            "SELECT 'venue' AS type, v.venue_name AS name, b.start_time AS startTime, b.end_time AS endTime, b.status, b.created_at AS createdAt ",
            "FROM venue_bookings b JOIN venues v ON b.venue_id = v.id ",
            "WHERE b.team_id = #{teamId} ",
            "ORDER BY b.created_at DESC LIMIT 10"
    })
    List<Map<String, Object>> getRecentVenueBookings(@Param("teamId") Integer teamId);
}
