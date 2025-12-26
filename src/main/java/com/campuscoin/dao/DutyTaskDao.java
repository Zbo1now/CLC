package com.campuscoin.dao;

import com.campuscoin.model.DutyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DutyTaskDao {

    @Select("SELECT id, activity_name AS taskName, start_time AS startTime, end_time AS endTime, max_participants AS requiredPeople, reward_coins AS rewardCoins, description AS taskDesc, location, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activities WHERE activity_type = 'DUTY' AND status != 'CANCELLED' ORDER BY start_time ASC")
    List<DutyTask> listAll();

    @Select("SELECT id, activity_name AS taskName, start_time AS startTime, end_time AS endTime, max_participants AS requiredPeople, reward_coins AS rewardCoins, description AS taskDesc, location, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activities WHERE id = #{id} AND activity_type = 'DUTY'")
    DutyTask findById(@Param("id") int id);

    @Select("SELECT id, activity_name AS taskName, start_time AS startTime, end_time AS endTime, max_participants AS requiredPeople, reward_coins AS rewardCoins, description AS taskDesc, location, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM activities WHERE id = #{id} AND activity_type = 'DUTY' FOR UPDATE")
    DutyTask findByIdForUpdate(@Param("id") int id);
}
