package com.campuscoin.dao.admin;

import com.campuscoin.model.ActivityParticipation;
import com.campuscoin.model.admin.AdminParticipationListItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 活动参与记录 DAO（管理员后台）
 */
@Mapper
public interface AdminParticipationDao {

    /**
     * 分页查询参与记录列表（带筛选）
     */
    @Select({
            "<script>",
            "SELECT ",
            "  p.id, ",
            "  p.activity_id AS activityId, ",
            "  a.activity_name AS activityName, ",
            "  p.team_id AS teamId, ",
            "  p.team_name AS teamName, ",
            "  p.apply_time AS applyTime, ",
            "  p.status, ",
            "  p.review_status AS reviewStatus, ",
            "  p.reviewed_by AS reviewedBy, ",
            "  p.reviewed_at AS reviewedAt, ",
            "  p.reject_reason AS rejectReason, ",
            "  p.coins_rewarded AS coinsRewarded, ",
            "  p.coins_rewarded_at AS coinsRewardedAt, ",
            "  p.completion_notes AS completionNotes ",
            "FROM activity_participations p ",
            "JOIN activities a ON p.activity_id = a.id ",
            "<where>",
            "  <if test='activityId != null'>",
            "    AND p.activity_id = #{activityId}",
            "  </if>",
            "  <if test='reviewStatus != null and reviewStatus != \"\"'>",
            "    AND p.review_status = #{reviewStatus}",
            "  </if>",
            "  <if test='teamName != null and teamName != \"\"'>",
            "    AND p.team_name LIKE CONCAT('%', #{teamName}, '%')",
            "  </if>",
            "</where>",
            "ORDER BY p.apply_time DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<AdminParticipationListItem> listPaged(@Param("activityId") Integer activityId,
                                               @Param("reviewStatus") String reviewStatus,
                                               @Param("teamName") String teamName,
                                               @Param("pageSize") int pageSize,
                                               @Param("offset") int offset);

    /**
     * 统计参与记录总数
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) ",
            "FROM activity_participations p ",
            "<where>",
            "  <if test='activityId != null'>",
            "    AND p.activity_id = #{activityId}",
            "  </if>",
            "  <if test='reviewStatus != null and reviewStatus != \"\"'>",
            "    AND p.review_status = #{reviewStatus}",
            "  </if>",
            "  <if test='teamName != null and teamName != \"\"'>",
            "    AND p.team_name LIKE CONCAT('%', #{teamName}, '%')",
            "  </if>",
            "</where>",
            "</script>"
    })
    int countByCondition(@Param("activityId") Integer activityId,
                        @Param("reviewStatus") String reviewStatus,
                        @Param("teamName") String teamName);

    /**
     * 根据ID查询参与记录
     */
    @Select("SELECT " +
            "  id, " +
            "  activity_id AS activityId, " +
            "  team_id AS teamId, " +
            "  team_name AS teamName, " +
            "  apply_time AS applyTime, " +
            "  status, " +
            "  review_status AS reviewStatus, " +
            "  reviewed_by AS reviewedBy, " +
            "  reviewed_at AS reviewedAt, " +
            "  reject_reason AS rejectReason, " +
            "  coins_rewarded AS coinsRewarded, " +
            "  coins_rewarded_at AS coinsRewardedAt, " +
            "  completion_notes AS completionNotes " +
            "FROM activity_participations " +
            "WHERE id = #{id}")
    ActivityParticipation findById(@Param("id") Integer id);

    /**
     * 审核通过参与申请
     */
    @Update("UPDATE activity_participations SET " +
            "  review_status = 'APPROVED', " +
            "  status = 'APPROVED', " +
            "  reviewed_by = #{reviewedBy}, " +
            "  reviewed_at = NOW(), " +
            "  coins_rewarded = #{coinsRewarded}, " +
            "  coins_rewarded_at = NOW() " +
            "WHERE id = #{id} AND review_status = 'PENDING'")
    int approve(@Param("id") Integer id, @Param("reviewedBy") String reviewedBy, @Param("coinsRewarded") Integer coinsRewarded);

    /**
     * 审核驳回参与申请
     */
    @Update("UPDATE activity_participations SET " +
            "  review_status = 'REJECTED', " +
            "  status = 'REJECTED', " +
            "  reviewed_by = #{reviewedBy}, " +
            "  reviewed_at = NOW(), " +
            "  reject_reason = #{rejectReason} " +
            "WHERE id = #{id} AND review_status = 'PENDING'")
    int reject(@Param("id") Integer id, @Param("rejectReason") String rejectReason, @Param("reviewedBy") String reviewedBy);

    /**
     * 批量审核通过
     */
    @Update({
            "<script>",
            "UPDATE activity_participations SET ",
            "  review_status = 'APPROVED', ",
            "  status = 'APPROVED', ",
            "  reviewed_by = #{reviewedBy}, ",
            "  reviewed_at = NOW() ",
            "WHERE id IN ",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "  AND review_status = 'PENDING'",
            "</script>"
    })
    int batchApprove(@Param("ids") List<Integer> ids, @Param("reviewedBy") String reviewedBy);

    /**
     * 标记为已完成并发放奖励
     */
    @Update("UPDATE activity_participations SET " +
            "  status = 'COMPLETED', " +
            "  coins_rewarded = #{coinsRewarded}, " +
            "  coins_rewarded_at = NOW() " +
            "WHERE id = #{id} AND review_status = 'APPROVED'")
    int markCompleted(@Param("id") Integer id, @Param("coinsRewarded") Integer coinsRewarded);

    /**
     * 批量标记为已完成并发放奖励
     */
    @Update({
            "<script>",
            "UPDATE activity_participations SET ",
            "  status = 'COMPLETED', ",
            "  coins_rewarded = #{coinsRewarded}, ",
            "  coins_rewarded_at = NOW() ",
            "WHERE id IN ",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "  AND review_status = 'APPROVED'",
            "</script>"
    })
    int batchMarkCompleted(@Param("ids") List<Integer> ids, @Param("coinsRewarded") Integer coinsRewarded);
}
