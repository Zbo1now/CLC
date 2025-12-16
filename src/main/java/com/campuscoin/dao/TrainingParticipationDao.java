package com.campuscoin.dao;

import com.campuscoin.model.TrainingParticipation;
import com.campuscoin.model.TrainingParticipationView;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TrainingParticipationDao {

    @Insert("INSERT INTO training_participations (event_id, team_id, proof_url, note, status) VALUES (#{eventId}, #{teamId}, #{proofUrl}, #{note}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(TrainingParticipation p);

    @Select("SELECT id, event_id AS eventId, team_id AS teamId, proof_url AS proofUrl, note, status, reward_coins AS rewardCoins, " +
            "reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM training_participations WHERE event_id = #{eventId} AND team_id = #{teamId} LIMIT 1")
    TrainingParticipation findByEventAndTeam(@Param("eventId") int eventId, @Param("teamId") int teamId);

    @Select("<script>" +
            "SELECT id, event_id AS eventId, team_id AS teamId, proof_url AS proofUrl, note, status, reward_coins AS rewardCoins, " +
            "reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM training_participations WHERE team_id = #{teamId} AND event_id IN " +
            "<foreach collection='eventIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<TrainingParticipation> listByTeamAndEventIds(@Param("teamId") int teamId, @Param("eventIds") List<Integer> eventIds);

    @Select("SELECT p.id, p.event_id AS eventId, p.team_id AS teamId, p.proof_url AS proofUrl, p.note, p.status, p.reward_coins AS rewardCoins, " +
            "p.reject_reason AS rejectReason, p.reviewed_by AS reviewedBy, p.reviewed_at AS reviewedAt, p.created_at AS createdAt, p.updated_at AS updatedAt, " +
            "e.event_name AS eventName, e.event_type AS eventType, e.start_time AS startTime, e.end_time AS endTime, e.location_mode AS locationMode, e.location_detail AS locationDetail, e.reward_coins AS eventRewardCoins " +
            "FROM training_participations p JOIN training_events e ON p.event_id = e.id " +
            "WHERE p.team_id = #{teamId} ORDER BY p.created_at DESC")
    List<TrainingParticipationView> listMyViews(@Param("teamId") int teamId);
}
