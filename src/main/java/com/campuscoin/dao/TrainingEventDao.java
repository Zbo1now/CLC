package com.campuscoin.dao;

import com.campuscoin.model.TrainingEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingEventDao {

    @Select("SELECT id, event_name AS eventName, event_type AS eventType, start_time AS startTime, end_time AS endTime, " +
            "location_mode AS locationMode, location_detail AS locationDetail, reward_coins AS rewardCoins, " +
            "require_checkin AS requireCheckin, description, publish_status AS publishStatus, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM training_events WHERE publish_status = 'PUBLISHED' ORDER BY start_time ASC")
    List<TrainingEvent> listPublished();

    @Select("SELECT id, event_name AS eventName, event_type AS eventType, start_time AS startTime, end_time AS endTime, " +
            "location_mode AS locationMode, location_detail AS locationDetail, reward_coins AS rewardCoins, " +
            "require_checkin AS requireCheckin, description, publish_status AS publishStatus, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM training_events WHERE id = #{id} LIMIT 1")
    TrainingEvent findById(@Param("id") int id);
}
