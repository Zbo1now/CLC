package com.campuscoin.dao;

import com.campuscoin.model.TrainingParticipation;
import com.campuscoin.model.TrainingParticipationView;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrainingParticipationDao {

    @Insert("INSERT INTO activity_participations (activity_id, team_id, team_name, completion_notes, status, review_status) " +
            "SELECT #{eventId}, #{teamId}, t.team_name, CONCAT('证明材料：', #{proofUrl}, '\n备注：', #{note}), 'APPLIED', #{status} FROM teams t WHERE t.id = #{teamId}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(TrainingParticipation p);

    @Select("SELECT id, activity_id AS eventId, team_id AS teamId, '' AS proofUrl, completion_notes AS note, review_status AS status, coins_rewarded AS rewardCoins, " +
            "reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activity_participations WHERE activity_id = #{eventId} AND team_id = #{teamId} LIMIT 1")
    TrainingParticipation findByEventAndTeam(@Param("eventId") int eventId, @Param("teamId") int teamId);

    @Select("<script>" +
            "SELECT id, activity_id AS eventId, team_id AS teamId, '' AS proofUrl, completion_notes AS note, review_status AS status, coins_rewarded AS rewardCoins, " +
            "reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activity_participations WHERE team_id = #{teamId} AND activity_id IN " +
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<TrainingParticipation> listByTeamAndEventIds(@Param("teamId") int teamId, @Param("eventIds") List<Integer> eventIds);

    @Select("SELECT p.id, p.activity_id AS eventId, p.team_id AS teamId, '' AS proofUrl, p.completion_notes AS note, p.review_status AS status, p.coins_rewarded AS rewardCoins, " +
            "p.reject_reason AS rejectReason, p.reviewed_by AS reviewedBy, p.reviewed_at AS reviewedAt, p.created_at AS createdAt, p.updated_at AS updatedAt, " +
            "a.activity_name AS eventName, a.activity_type AS eventType, a.start_time AS startTime, a.end_time AS endTime, '' AS locationMode, a.location AS locationDetail, a.reward_coins AS eventRewardCoins " +
            "FROM activity_participations p JOIN activities a ON p.activity_id = a.id " +
            "WHERE p.team_id = #{teamId} AND a.activity_type = 'TRAINING' ORDER BY p.created_at DESC")
    List<TrainingParticipationView> listMyViews(@Param("teamId") int teamId);
}
