package com.campuscoin.dao.admin;

import com.campuscoin.model.Activity;
import com.campuscoin.model.admin.AdminActivityListItem;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.util.List;

/**
 * 活动管理 DAO（管理员后台）
 */
@Mapper
public interface AdminActivityDao {

    /**
     * 分页查询活动列表（带筛选和统计）
     */
    @Select({
            "<script>",
            "SELECT ",
            "  a.id, ",
            "  a.activity_name AS activityName, ",
            "  a.activity_type AS activityType, ",
            "  a.description, ",
            "  a.location, ",
            "  a.start_time AS startTime, ",
            "  a.end_time AS endTime, ",
            "  a.reward_coins AS rewardCoins, ",
            "  a.participation_type AS participationType, ",
            "  a.max_participants AS maxParticipants, ",
            "  a.current_participants AS currentParticipants, ",
            "  a.status, ",
            "  a.created_by AS createdBy, ",
            "  a.created_at AS createdAt, ",
            "  COALESCE(SUM(CASE WHEN p.review_status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pendingCount, ",
            "  COALESCE(SUM(CASE WHEN p.review_status = 'APPROVED' THEN 1 ELSE 0 END), 0) AS approvedCount, ",
            "  COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedCount ",
            "FROM activities a ",
            "LEFT JOIN activity_participations p ON a.id = p.activity_id ",
            "<where>",
            "  <if test='activityType != null and activityType != \"\"'>",
            "    AND a.activity_type = #{activityType}",
            "  </if>",
            "  <if test='status != null and status != \"\"'>",
            "    AND a.status = #{status}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (a.activity_name LIKE CONCAT('%', #{keyword}, '%') OR a.location LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "</where>",
            "GROUP BY a.id ",
            "ORDER BY a.created_at DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<AdminActivityListItem> listPaged(@Param("activityType") String activityType,
                                          @Param("status") String status,
                                          @Param("keyword") String keyword,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    /**
     * 统计活动总数
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) ",
            "FROM activities a ",
            "<where>",
            "  <if test='activityType != null and activityType != \"\"'>",
            "    AND a.activity_type = #{activityType}",
            "  </if>",
            "  <if test='status != null and status != \"\"'>",
            "    AND a.status = #{status}",
            "  </if>",
            "  <if test='keyword != null and keyword != \"\"'>",
            "    AND (a.activity_name LIKE CONCAT('%', #{keyword}, '%') OR a.location LIKE CONCAT('%', #{keyword}, '%'))",
            "  </if>",
            "</where>",
            "</script>"
    })
    int countByCondition(@Param("activityType") String activityType,
                        @Param("status") String status,
                        @Param("keyword") String keyword);

    /**
     * 根据ID查询活动详情
     */
    @Select("SELECT " +
            "  id, " +
            "  activity_name AS activityName, " +
            "  activity_type AS activityType, " +
            "  description, " +
            "  location, " +
            "  start_time AS startTime, " +
            "  end_time AS endTime, " +
            "  reward_coins AS rewardCoins, " +
            "  participation_type AS participationType, " +
            "  max_participants AS maxParticipants, " +
            "  current_participants AS currentParticipants, " +
            "  status, " +
            "  created_by AS createdBy, " +
            "  created_at AS createdAt, " +
            "  updated_at AS updatedAt " +
            "FROM activities " +
            "WHERE id = #{id}")
    Activity findById(@Param("id") Integer id);

    /**
     * 创建活动
     */
    @Insert("INSERT INTO activities (" +
            "  activity_name, activity_type, description, location, " +
            "  start_time, end_time, reward_coins, participation_type, " +
            "  max_participants, status, created_by" +
            ") VALUES (" +
            "  #{activityName}, #{activityType}, #{description}, #{location}, " +
            "  #{startTime}, #{endTime}, #{rewardCoins}, #{participationType}, " +
            "  #{maxParticipants}, #{status}, #{createdBy}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(Activity activity);

    /**
     * 更新活动
     */
    @Update("UPDATE activities SET " +
            "  activity_name = #{activityName}, " +
            "  activity_type = #{activityType}, " +
            "  description = #{description}, " +
            "  location = #{location}, " +
            "  start_time = #{startTime}, " +
            "  end_time = #{endTime}, " +
            "  reward_coins = #{rewardCoins}, " +
            "  participation_type = #{participationType}, " +
            "  max_participants = #{maxParticipants} " +
            "WHERE id = #{id}")
    int update(Activity activity);

    /**
     * 更新活动状态
     */
    @Update("UPDATE activities SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 删除活动（仅未开始的活动）
     */
    @Delete("DELETE FROM activities WHERE id = #{id} AND status = 'NOT_STARTED'")
    int deleteById(@Param("id") Integer id);

    /**
     * 增加当前参与人数
     */
    @Update("UPDATE activities SET current_participants = current_participants + 1 WHERE id = #{id}")
    int incrementParticipants(@Param("id") Integer id);

    /**
     * 减少当前参与人数
     */
    @Update("UPDATE activities SET current_participants = current_participants - 1 WHERE id = #{id} AND current_participants > 0")
    int decrementParticipants(@Param("id") Integer id);

    /**
     * 自动更新活动状态（根据时间）
     */
    @Update("UPDATE activities SET status = " +
            "  CASE " +
            "    WHEN start_time > NOW() THEN 'NOT_STARTED' " +
            "    WHEN end_time < NOW() THEN 'FINISHED' " +
            "    ELSE 'ONGOING' " +
            "  END " +
            "WHERE status IN ('NOT_STARTED', 'ONGOING') " +
            "  AND status != CASE " +
            "    WHEN start_time > NOW() THEN 'NOT_STARTED' " +
            "    WHEN end_time < NOW() THEN 'FINISHED' " +
            "    ELSE 'ONGOING' " +
            "  END")
    int autoUpdateStatus();
}
