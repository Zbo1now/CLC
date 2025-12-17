package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminDailyFlow;
import com.campuscoin.model.admin.AdminHeatmapPoint;
import com.campuscoin.model.admin.NameCount;
import com.campuscoin.model.admin.TeamAmount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface AdminDashboardDao {

    @Select({
            "<script>",
            "SELECT txn_type AS name, SUM(amount) AS cnt ",
            "FROM transactions ",
            "WHERE created_at &gt;= #{start} AND created_at &lt; #{end} ",
            "  AND txn_type IN ",
            "  <foreach collection='types' item='t' open='(' separator=',' close=')'>",
            "    #{t}",
            "  </foreach>",
            "GROUP BY txn_type",
            "</script>"
    })
    List<NameCount> sumAmountByTxnType(@Param("start") Timestamp start,
                                       @Param("end") Timestamp end,
                                       @Param("types") List<String> types);

    @Select({
            "<script>",
            "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS date, ",
            "  SUM(CASE WHEN amount &gt; 0 AND txn_type IN ",
            "    <foreach collection='inTypes' item='t' open='(' separator=',' close=')'>",
            "      #{t}",
            "    </foreach>",
            "  THEN amount ELSE 0 END) AS inflow,",
            "  SUM(CASE WHEN amount &lt; 0 AND txn_type IN ",
            "    <foreach collection='outTypes' item='t' open='(' separator=',' close=')'>",
            "      #{t}",
            "    </foreach>",
            "  THEN -amount ELSE 0 END) AS outflow ",
            "FROM transactions ",
            "WHERE created_at &gt;= #{start} AND created_at &lt; #{end} ",
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m-%d') ",
            "ORDER BY DATE_FORMAT(created_at, '%Y-%m-%d')",
            "</script>"
    })
    List<AdminDailyFlow> dailyFlow(@Param("start") Timestamp start,
                                  @Param("end") Timestamp end,
                                  @Param("inTypes") List<String> inTypes,
                                  @Param("outTypes") List<String> outTypes);

    @Select({
            "<script>",
            "SELECT t.id AS teamId, t.team_name AS teamName, SUM(x.amount) AS amount ",
            "FROM teams t ",
            "JOIN transactions x ON x.team_id = t.id ",
            "WHERE x.created_at &gt;= #{start} AND x.created_at &lt; #{end} ",
            "  AND x.amount &gt; 0 ",
            "  AND x.txn_type IN ",
            "  <foreach collection='types' item='t' open='(' separator=',' close=')'>",
            "    #{t}",
            "  </foreach>",
            "GROUP BY t.id, t.team_name ",
            "ORDER BY amount DESC ",
            "LIMIT #{limit}",
            "</script>"
    })
    List<TeamAmount> topTeamsGain(@Param("start") Timestamp start,
                                  @Param("end") Timestamp end,
                                  @Param("types") List<String> types,
                                  @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT t.id AS teamId, t.team_name AS teamName, SUM(-x.amount) AS amount ",
            "FROM teams t ",
            "JOIN transactions x ON x.team_id = t.id ",
            "WHERE x.created_at &gt;= #{start} AND x.created_at &lt; #{end} ",
            "  AND x.amount &lt; 0 ",
            "  AND x.txn_type IN ",
            "  <foreach collection='types' item='t' open='(' separator=',' close=')'>",
            "    #{t}",
            "  </foreach>",
            "GROUP BY t.id, t.team_name ",
            "ORDER BY amount DESC ",
            "LIMIT #{limit}",
            "</script>"
    })
    List<TeamAmount> topTeamsSpend(@Param("start") Timestamp start,
                                   @Param("end") Timestamp end,
                                   @Param("types") List<String> types,
                                   @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM teams")
    int totalTeams();

    @Select("SELECT COUNT(*) FROM teams WHERE face_image IS NOT NULL AND face_image <> ''")
    int activatedTeams();

    @Select("SELECT COUNT(*) FROM achievement_submissions WHERE status = 'PENDING'")
    int pendingAchievements();

    @Select("SELECT COUNT(*) FROM device_bookings WHERE status IN ('RESERVED','IN_USE') AND start_at >= NOW() AND start_at < DATE_ADD(NOW(), INTERVAL 24 HOUR)")
    int upcomingDeviceBookings();

    @Select("SELECT COUNT(*) FROM venue_bookings WHERE status IN ('BOOKED','IN_USE') AND start_time >= NOW() AND start_time < DATE_ADD(NOW(), INTERVAL 24 HOUR)")
    int upcomingVenueBookings();

    @Select("SELECT COUNT(*) FROM achievement_submissions WHERE created_at >= #{start} AND created_at < #{end}")
    int countAchievementSubmissions(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT COUNT(*) FROM duty_task_signups WHERE signed_at >= #{start} AND signed_at < #{end}")
    int countDutySignups(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT COUNT(*) FROM training_participations WHERE created_at >= #{start} AND created_at < #{end}")
    int countTrainingParticipations(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT COUNT(*) FROM transactions WHERE created_at >= #{start} AND created_at < #{end} AND txn_type = 'CHECKIN' AND amount > 0")
    int countCheckinActions(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT IFNULL(SUM(reward_coins), 0) FROM training_participations WHERE status = 'APPROVED' AND reviewed_at IS NOT NULL AND reviewed_at >= #{start} AND reviewed_at < #{end}")
    Integer sumTrainingApprovedReward(@Param("start") Timestamp start, @Param("end") Timestamp end);

        @Select("SELECT DATE_FORMAT(reviewed_at, '%Y-%m-%d') AS date, IFNULL(SUM(reward_coins),0) AS inflow, 0 AS outflow FROM training_participations WHERE status='APPROVED' AND reviewed_at IS NOT NULL AND reviewed_at >= #{start} AND reviewed_at < #{end} GROUP BY DATE_FORMAT(reviewed_at, '%Y-%m-%d') ORDER BY DATE_FORMAT(reviewed_at, '%Y-%m-%d')")
    List<AdminDailyFlow> dailyTrainingInflow(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT d.device_name AS name, COUNT(*) AS cnt FROM device_bookings b JOIN devices d ON b.device_id = d.id WHERE b.created_at >= #{start} AND b.created_at < #{end} GROUP BY d.device_name")
    List<NameCount> popularDevices(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT CONCAT('工位 ', w.station_code) AS name, COUNT(*) AS cnt FROM workstation_leases l JOIN workstations w ON l.workstation_id = w.id WHERE l.created_at >= #{start} AND l.created_at < #{end} GROUP BY w.station_code")
    List<NameCount> popularWorkstations(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT e.equipment_name AS name, COUNT(*) AS cnt FROM equipment_loans l JOIN equipments e ON l.equipment_id = e.id WHERE l.created_at >= #{start} AND l.created_at < #{end} GROUP BY e.equipment_name")
    List<NameCount> popularEquipments(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select("SELECT v.venue_name AS name, COUNT(*) AS cnt FROM venue_bookings b JOIN venues v ON b.venue_id = v.id WHERE b.created_at >= #{start} AND b.created_at < #{end} GROUP BY v.venue_name")
    List<NameCount> popularVenues(@Param("start") Timestamp start, @Param("end") Timestamp end);

    @Select({
            "<script>",
            "SELECT WEEKDAY(ts) AS dow, HOUR(ts) AS hour, COUNT(*) AS cnt ",
            "FROM (",
            "  SELECT created_at AS ts FROM transactions WHERE txn_type = 'CHECKIN' AND created_at &gt;= #{start} AND created_at &lt; #{end}",
            "  UNION ALL ",
            "  SELECT created_at AS ts FROM achievement_submissions WHERE created_at &gt;= #{start} AND created_at &lt; #{end}",
            "  UNION ALL ",
            "  SELECT signed_at AS ts FROM duty_task_signups WHERE signed_at &gt;= #{start} AND signed_at &lt; #{end}",
            "  UNION ALL ",
            "  SELECT created_at AS ts FROM training_participations WHERE created_at &gt;= #{start} AND created_at &lt; #{end}",
            ") u ",
            "GROUP BY WEEKDAY(ts), HOUR(ts)",
            "</script>"
    })
    List<AdminHeatmapPoint> activityHeatmap(@Param("start") Timestamp start, @Param("end") Timestamp end);
}
