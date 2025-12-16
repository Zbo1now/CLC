package com.campuscoin.dao;

import com.campuscoin.model.Checkin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CheckinDao {

    private final JdbcTemplate jdbcTemplate;

    public CheckinDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean create(Checkin checkin) {
        String sql = "INSERT INTO checkins (team_id, checkin_date, coins_earned) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, checkin.getTeamId(), checkin.getCheckinDate(), checkin.getCoinsEarned()) == 1;
    }

    public List<Checkin> findByTeamAndMonth(int teamId, int year, int month) {
        String sql = "SELECT * FROM checkins WHERE team_id = ? AND YEAR(checkin_date) = ? AND MONTH(checkin_date) = ?";
        return jdbcTemplate.query(sql, new CheckinRowMapper(), teamId, year, month);
    }
    
    public boolean hasCheckedInToday(int teamId, java.sql.Date date) {
        String sql = "SELECT COUNT(*) FROM checkins WHERE team_id = ? AND checkin_date = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, teamId, date);
        return count != null && count > 0;
    }

    private static class CheckinRowMapper implements RowMapper<Checkin> {
        @Override
        public Checkin mapRow(ResultSet rs, int rowNum) throws SQLException {
            Checkin c = new Checkin();
            c.setId(rs.getInt("id"));
            c.setTeamId(rs.getInt("team_id"));
            c.setCheckinDate(rs.getDate("checkin_date"));
            c.setCoinsEarned(rs.getInt("coins_earned"));
            c.setCreatedAt(rs.getTimestamp("created_at"));
            return c;
        }
    }
}
