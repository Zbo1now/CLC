package com.campuscoin.dao;

import com.campuscoin.model.VenueBooking;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VenueBookingDao {

    @Insert("INSERT INTO venue_bookings(team_id, venue_id, start_time, end_time, status, held_cost) " +
            "VALUES(#{teamId}, #{venueId}, #{startTime}, #{endTime}, #{status}, #{heldCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(VenueBooking booking);

    @Select("SELECT COUNT(1) FROM venue_bookings " +
            "WHERE venue_id = #{venueId} " +
            "AND status IN ('BOOKED','IN_USE') " +
            "AND NOT (end_time <= #{startTime} OR start_time >= #{endTime})")
    int countOverlappingActive(@Param("venueId") int venueId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE team_id = #{teamId} AND status IN ('BOOKED','IN_USE') ORDER BY start_time DESC LIMIT 1")
    VenueBooking findMyActive(@Param("teamId") int teamId);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE team_id = #{teamId} ORDER BY created_at DESC LIMIT 1")
    VenueBooking findMyLatest(@Param("teamId") int teamId);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE id = #{id}")
    VenueBooking findById(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE id = #{id} FOR UPDATE")
    VenueBooking findByIdForUpdate(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE venue_id = #{venueId} AND status IN ('BOOKED','IN_USE') " +
            "AND NOT (end_time <= #{startTime} OR start_time >= #{endTime}) ORDER BY start_time ASC")
    List<VenueBooking> listOverlappingActive(@Param("venueId") int venueId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    @Update("UPDATE venue_bookings SET status = 'IN_USE', confirmed_at = NOW() WHERE id = #{id} AND status = 'BOOKED'")
    int markInUse(@Param("id") int id);

    @Update("UPDATE venue_bookings SET status = #{status}, cancelled_at = NOW() WHERE id = #{id} AND status = 'BOOKED'")
    int markCancelled(@Param("id") int id, @Param("status") String status);

    @Update("UPDATE venue_bookings SET status = 'CANCELLED', cancelled_at = NOW() WHERE id = #{id} AND status = 'BOOKED'")
    int markUserCancelled(@Param("id") int id);

    @Update("UPDATE venue_bookings SET status = 'COMPLETED', finished_at = NOW() WHERE id = #{id} AND status = 'IN_USE'")
    int markCompleted(@Param("id") int id);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE status = 'BOOKED' AND start_time <= #{deadline} ORDER BY start_time ASC LIMIT #{limit}")
    List<VenueBooking> listBookedBefore(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE status = 'BOOKED' AND end_time <= #{now} ORDER BY end_time ASC LIMIT #{limit}")
    List<VenueBooking> listBookedEndedBefore(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT id, team_id AS teamId, venue_id AS venueId, start_time AS startTime, end_time AS endTime, status, held_cost AS heldCost, " +
            "confirmed_at AS confirmedAt, cancelled_at AS cancelledAt, finished_at AS finishedAt, created_at AS createdAt " +
            "FROM venue_bookings WHERE status = 'IN_USE' AND end_time <= #{now} ORDER BY end_time ASC LIMIT #{limit}")
    List<VenueBooking> listInUseEndedBefore(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Select("SELECT status FROM venue_bookings " +
            "WHERE venue_id = #{venueId} AND status IN ('BOOKED','IN_USE') " +
            "AND start_time <= #{now} AND end_time > #{now} " +
            "ORDER BY (status = 'IN_USE') DESC, start_time DESC LIMIT 1")
    String findCurrentStatus(@Param("venueId") int venueId, @Param("now") LocalDateTime now);
}
