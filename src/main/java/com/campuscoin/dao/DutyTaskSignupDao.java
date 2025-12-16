package com.campuscoin.dao;

import com.campuscoin.model.DutyTaskSignupCount;
import com.campuscoin.model.DutyTaskSignup;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DutyTaskSignupDao {

    @Insert("INSERT INTO duty_task_signups (task_id, team_id, signup_status) VALUES (#{taskId}, #{teamId}, #{signupStatus})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(DutyTaskSignup signup);

    @Select("SELECT id, task_id AS taskId, team_id AS teamId, signup_status AS signupStatus, signed_at AS signedAt, confirmed_at AS confirmedAt, reward_amount AS rewardAmount " +
            "FROM duty_task_signups WHERE task_id = #{taskId} AND team_id = #{teamId} LIMIT 1")
    DutyTaskSignup findByTaskAndTeam(@Param("taskId") int taskId, @Param("teamId") int teamId);

    @Select("SELECT id, task_id AS taskId, team_id AS teamId, signup_status AS signupStatus, signed_at AS signedAt, confirmed_at AS confirmedAt, reward_amount AS rewardAmount " +
            "FROM duty_task_signups WHERE id = #{id} FOR UPDATE")
    DutyTaskSignup findByIdForUpdate(@Param("id") int id);

    @Select("SELECT COUNT(1) FROM duty_task_signups WHERE task_id = #{taskId} AND signup_status IN ('SIGNED','COMPLETED')")
    int countActiveByTask(@Param("taskId") int taskId);

    @Select("<script>" +
            "SELECT id, task_id AS taskId, team_id AS teamId, signup_status AS signupStatus, signed_at AS signedAt, confirmed_at AS confirmedAt, reward_amount AS rewardAmount " +
            "FROM duty_task_signups WHERE team_id = #{teamId} AND task_id IN " +
            "<foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<DutyTaskSignup> listByTeamAndTaskIds(@Param("teamId") int teamId, @Param("taskIds") List<Integer> taskIds);

    @Select("<script>" +
            "SELECT task_id AS taskId, COUNT(1) AS cnt " +
            "FROM duty_task_signups " +
            "WHERE task_id IN <foreach collection='taskIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND signup_status IN ('SIGNED','COMPLETED') " +
            "GROUP BY task_id" +
            "</script>")
    @Results({
            @Result(property = "taskId", column = "taskId"),
            @Result(property = "count", column = "cnt")
    })
    List<DutyTaskSignupCount> countActiveGrouped(@Param("taskIds") List<Integer> taskIds);

    @Update("UPDATE duty_task_signups SET signup_status = #{status}, confirmed_at = NOW(), reward_amount = #{rewardAmount} WHERE id = #{id}")
    int confirm(@Param("id") int id, @Param("status") String status, @Param("rewardAmount") int rewardAmount);
}
