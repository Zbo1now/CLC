package com.campuscoin.dao;

import com.campuscoin.model.DutyTaskSignupCount;
import com.campuscoin.model.DutyTaskSignup;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DutyTaskSignupDao {

    @Insert("INSERT INTO activity_participations (activity_id, team_id, team_name, status, review_status) " +
            "SELECT #{taskId}, #{teamId}, t.team_name, 'APPLIED', 'PENDING' FROM teams t WHERE t.id = #{teamId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(DutyTaskSignup signup);

    @Select("SELECT id, activity_id AS taskId, team_id AS teamId, review_status AS signupStatus, apply_time AS signedAt, reviewed_at AS confirmedAt, coins_rewarded AS rewardAmount " +
            "FROM activity_participations WHERE activity_id = #{taskId} AND team_id = #{teamId} LIMIT 1")
    DutyTaskSignup findByTaskAndTeam(@Param("taskId") int taskId, @Param("teamId") int teamId);

    @Select("SELECT id, activity_id AS taskId, team_id AS teamId, review_status AS signupStatus, apply_time AS signedAt, reviewed_at AS confirmedAt, coins_rewarded AS rewardAmount " +
            "FROM activity_participations WHERE id = #{id} FOR UPDATE")
    DutyTaskSignup findByIdForUpdate(@Param("id") int id);

    @Select("SELECT COUNT(1) FROM activity_participations WHERE activity_id = #{taskId} AND status != 'CANCELLED' AND review_status != 'REJECTED'")
    int countActiveByTask(@Param("taskId") int taskId);

    @Select("<script>" +
            "SELECT id, activity_id AS taskId, team_id AS teamId, review_status AS signupStatus, apply_time AS signedAt, reviewed_at AS confirmedAt, coins_rewarded AS rewardAmount " +
            "FROM activity_participations WHERE team_id = #{teamId} AND activity_id IN " +
            "<foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<DutyTaskSignup> listByTeamAndTaskIds(@Param("teamId") int teamId, @Param("taskIds") List<Integer> taskIds);

    @Select("<script>" +
            "SELECT activity_id AS taskId, COUNT(1) AS cnt " +
            "FROM activity_participations " +
            "WHERE activity_id IN <foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND review_status = 'APPROVED' AND status IN ('APPLIED','IN_PROGRESS','COMPLETED') " +
            "GROUP BY activity_id" +
            "</script>")
    @Results({
            @Result(property = "taskId", column = "taskId"),
            @Result(property = "count", column = "cnt")
    })
    List<DutyTaskSignupCount> countActiveGrouped(@Param("taskIds") List<Integer> taskIds);

    @Update("UPDATE activity_participations SET review_status = 'APPROVED', status = #{status}, reviewed_at = NOW(), coins_rewarded = #{rewardAmount} WHERE id = #{id}")
    int confirm(@Param("id") int id, @Param("status") String status, @Param("rewardAmount") int rewardAmount);

    @Update("UPDATE activity_participations SET status = 'CANCELLED', review_status = 'REJECTED', updated_at = NOW() WHERE id = #{id} AND team_id = #{teamId} AND status = 'APPLIED'")
    int cancelByTeam(@Param("id") int id, @Param("teamId") int teamId);
}
