package com.campuscoin.dao;

import com.campuscoin.model.Equipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EquipmentDao {

    @Select("SELECT id, equipment_name AS equipmentName, equipment_type AS equipmentType, rate_per_day AS ratePerDay, status, created_at AS createdAt " +
            "FROM equipments ORDER BY id ASC")
    List<Equipment> listAll();

    @Select("SELECT id, equipment_name AS equipmentName, equipment_type AS equipmentType, rate_per_day AS ratePerDay, status, created_at AS createdAt " +
            "FROM equipments WHERE id = #{id}")
    Equipment findById(@Param("id") int id);

    @Select("SELECT id, equipment_name AS equipmentName, equipment_type AS equipmentType, rate_per_day AS ratePerDay, status, created_at AS createdAt " +
            "FROM equipments WHERE id = #{id} FOR UPDATE")
    Equipment findByIdForUpdate(@Param("id") int id);
}
