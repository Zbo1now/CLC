package com.campuscoin.dao;

import com.campuscoin.model.Venue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VenueDao {

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues ORDER BY id ASC")
    List<Venue> listAll();

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues WHERE id = #{id}")
    Venue findById(@Param("id") int id);

    @Select("SELECT id, venue_name AS venueName, venue_type AS venueType, rate_per_hour AS ratePerHour, status, created_at AS createdAt, updated_at AS updatedAt FROM venues WHERE id = #{id} FOR UPDATE")
    Venue findByIdForUpdate(@Param("id") int id);
}
