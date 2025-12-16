package com.campuscoin.dao;

import com.campuscoin.model.Team;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface TeamDao {

    Logger logger = LoggerFactory.getLogger(TeamDao.class);

    @Select("SELECT id, team_name AS teamName, password_hash AS passwordHash, contact_name AS contactName, contact_phone AS contactPhone, face_image AS faceImage, balance, current_streak AS currentStreak, last_checkin_date AS lastCheckinDate, created_at AS createdAt FROM teams WHERE team_name = #{teamName}")
    Team findByNameInternal(@Param("teamName") String teamName);

    default Team findByName(String teamName) {
        Team team = findByNameInternal(teamName);
        if (logger.isInfoEnabled()) {
            logger.info("查询 Team.findByName -> teamName={}, result={}", teamName, describe(team));
        }
        return team;
    }

    @Insert("INSERT INTO teams (team_name, password_hash, contact_name, contact_phone, face_image, balance, current_streak, last_checkin_date) VALUES (#{teamName}, #{passwordHash}, #{contactName}, #{contactPhone}, #{faceImage}, #{balance}, #{currentStreak}, #{lastCheckinDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createTeam(Team team);

    @Update("UPDATE teams SET face_image = #{faceImageUrl} WHERE team_name = #{teamName}")
    int updateFaceImage(@Param("teamName") String teamName,
                        @Param("faceImageUrl") String faceImageUrl);

    @Update("UPDATE teams SET balance = #{newBalance}, current_streak = #{newStreak}, last_checkin_date = #{lastCheckinDate} WHERE id = #{teamId}")
    int updateCheckinStatus(@Param("teamId") int teamId,
                            @Param("newBalance") int newBalance,
                            @Param("newStreak") int newStreak,
                            @Param("lastCheckinDate") java.sql.Date lastCheckinDate);

    /**
     * 原子扣费：仅当余额足够时才扣减，返回受影响行数。
     * 用于租赁/续租等并发场景，避免先查余额再扣费带来的竞态。
     */
    @Update("UPDATE teams SET balance = balance - #{amount} WHERE id = #{teamId} AND balance >= #{amount}")
    int deductBalanceIfEnough(@Param("teamId") int teamId, @Param("amount") int amount);

    static String describe(Team team) {
        if (team == null) {
            return "null";
        }
        return String.format("id=%d, face=%s, balance=%d, streak=%d", team.getId(),
                team.getFaceImage(), team.getBalance(), team.getCurrentStreak());
    }
}
