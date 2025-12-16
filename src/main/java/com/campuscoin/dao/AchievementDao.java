package com.campuscoin.dao;

import com.campuscoin.model.AchievementSubmission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AchievementDao {

    @Insert("INSERT INTO achievement_submissions (team_id, category, sub_type, title, proof_url, description, status) " +
            "VALUES (#{teamId}, #{category}, #{subType}, #{title}, #{proofUrl}, #{description}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(AchievementSubmission submission);

    @Select("SELECT id, team_id AS teamId, category, sub_type AS subType, title, proof_url AS proofUrl, description, status, " +
            "reward_coins AS rewardCoins, reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM achievement_submissions WHERE team_id = #{teamId} ORDER BY created_at DESC")
    List<AchievementSubmission> listByTeam(@Param("teamId") int teamId);

    @Select("SELECT id, team_id AS teamId, category, sub_type AS subType, title, proof_url AS proofUrl, description, status, " +
            "reward_coins AS rewardCoins, reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM achievement_submissions WHERE id = #{id}")
    AchievementSubmission findById(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, category, sub_type AS subType, title, proof_url AS proofUrl, description, status, " +
            "reward_coins AS rewardCoins, reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM achievement_submissions WHERE id = #{id} FOR UPDATE")
    AchievementSubmission findByIdForUpdate(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, category, sub_type AS subType, title, proof_url AS proofUrl, description, status, " +
            "reward_coins AS rewardCoins, reject_reason AS rejectReason, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM achievement_submissions " +
            "WHERE (#{status} IS NULL OR status = #{status}) ORDER BY created_at DESC")
    List<AchievementSubmission> listAll(@Param("status") String status);

    @Update("UPDATE achievement_submissions SET status='APPROVED', reward_coins=#{rewardCoins}, reviewed_by=#{reviewedBy}, reviewed_at=NOW() " +
            "WHERE id=#{id} AND status='PENDING'")
    int approve(@Param("id") int id, @Param("rewardCoins") int rewardCoins, @Param("reviewedBy") String reviewedBy);

    @Update("UPDATE achievement_submissions SET status='REJECTED', reject_reason=#{rejectReason}, reviewed_by=#{reviewedBy}, reviewed_at=NOW() " +
            "WHERE id=#{id} AND status='PENDING'")
    int reject(@Param("id") int id, @Param("rejectReason") String rejectReason, @Param("reviewedBy") String reviewedBy);
}
