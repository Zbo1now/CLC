package com.campuscoin.dao;

import com.campuscoin.model.DutyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DutyTaskDao {

    @Select("SELECT id, task_name AS taskName, start_time AS startTime, end_time AS endTime, required_people AS requiredPeople, reward_coins AS rewardCoins, task_desc AS taskDesc, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM duty_tasks ORDER BY start_time ASC")
    List<DutyTask> listAll();

    @Select("SELECT id, task_name AS taskName, start_time AS startTime, end_time AS endTime, required_people AS requiredPeople, reward_coins AS rewardCoins, task_desc AS taskDesc, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM duty_tasks WHERE id = #{id}")
    DutyTask findById(@Param("id") int id);

    @Select("SELECT id, task_name AS taskName, start_time AS startTime, end_time AS endTime, required_people AS requiredPeople, reward_coins AS rewardCoins, task_desc AS taskDesc, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM duty_tasks WHERE id = #{id} FOR UPDATE")
    DutyTask findByIdForUpdate(@Param("id") int id);
}
