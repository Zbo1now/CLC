package com.campuscoin.dao;

import com.campuscoin.model.Checkin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.sql.Date;
import java.util.List;

@Mapper
public interface CheckinDao {

    @Insert("INSERT INTO checkins (team_id, checkin_date, coins_earned) VALUES (#{teamId}, #{checkinDate}, #{coinsEarned})")
    int create(Checkin checkin);

    // 显式 alias 下划线字段为驼峰，避免依赖 mybatis mapUnderscoreToCamelCase 配置
    @Select("SELECT id, team_id AS teamId, checkin_date AS checkinDate, coins_earned AS coinsEarned, created_at AS createdAt FROM checkins WHERE team_id = #{teamId} AND YEAR(checkin_date) = #{year} AND MONTH(checkin_date) = #{month}")
    List<Checkin> findByTeamAndMonth(@Param("teamId") int teamId,
                                     @Param("year") int year,
                                     @Param("month") int month);

    @Select("SELECT COUNT(*) FROM checkins WHERE team_id = #{teamId} AND checkin_date = #{date}")
    int countCheckinsOnDate(@Param("teamId") int teamId, @Param("date") Date date);

    default boolean hasCheckedInToday(int teamId, Date date) {
        return countCheckinsOnDate(teamId, date) > 0;
    }
}
