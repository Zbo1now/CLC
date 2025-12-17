package com.campuscoin.dao;

import com.campuscoin.model.TrainingEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingEventDao {

    @Select("SELECT id, activity_name AS eventName, activity_type AS eventType, start_time AS startTime, end_time AS endTime, " +
            "location, location AS locationDetail, reward_coins AS rewardCoins, " +
            "0 AS requireCheckin, description, status AS publishStatus, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activities WHERE activity_type = 'TRAINING' AND status != 'CANCELLED' ORDER BY start_time ASC")
    List<TrainingEvent> listPublished();

    @Select("SELECT id, activity_name AS eventName, activity_type AS eventType, start_time AS startTime, end_time AS endTime, " +
            "location, location AS locationDetail, reward_coins AS rewardCoins, " +
            "0 AS requireCheckin, description, status AS publishStatus, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activities WHERE id = #{id} AND activity_type = 'TRAINING' LIMIT 1")
    TrainingEvent findById(@Param("id") int id);
}
