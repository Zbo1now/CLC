package com.campuscoin.dao;

import com.campuscoin.model.Team;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class TeamDao {

    private final JdbcTemplate jdbcTemplate;

    public TeamDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Team> findByName(String teamName) {
        String sql = "SELECT id, team_name, password_hash, contact_name, contact_phone, face_image, balance, current_streak, last_checkin_date, created_at FROM teams WHERE team_name = ?";
        return jdbcTemplate.query(sql, new TeamRowMapper(), teamName).stream().findFirst();
    }

    public boolean createTeam(Team team) {
        String sql = "INSERT INTO teams (team_name, password_hash, contact_name, contact_phone, face_image, balance, current_streak) VALUES (?, ?, ?, ?, ?, ?, 0)";
        int rows = jdbcTemplate.update(sql,
                team.getTeamName(),
                team.getPasswordHash(),
            team.getContactName(),
            team.getContactPhone(),
            team.getFaceImage(),
            team.getBalance());
        return rows == 1;
    }

    public boolean updateFaceImage(String teamName, String faceImageUrl) {
        String sql = "UPDATE teams SET face_image = ? WHERE team_name = ?";
        return jdbcTemplate.update(sql, faceImageUrl, teamName) == 1;
    }

    public boolean updateCheckinStatus(int teamId, int newBalance, int newStreak, java.sql.Date checkinDate) {
        String sql = "UPDATE teams SET balance = ?, current_streak = ?, last_checkin_date = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newBalance, newStreak, checkinDate, teamId) == 1;
    }

    private static class TeamRowMapper implements RowMapper<Team> {
        @Override
        public Team mapRow(ResultSet rs, int rowNum) throws SQLException {
            Team t = new Team();
            t.setId(rs.getInt("id"));
            t.setTeamName(rs.getString("team_name"));
            t.setPasswordHash(rs.getString("password_hash"));
            t.setContactName(rs.getString("contact_name"));
            t.setContactPhone(rs.getString("contact_phone"));
            t.setFaceImage(rs.getString("face_image"));
            t.setBalance(rs.getInt("balance"));
            t.setCurrentStreak(rs.getInt("current_streak"));
            t.setLastCheckinDate(rs.getDate("last_checkin_date"));
            t.setCreatedAt(rs.getTimestamp("created_at"));
            return t;
        }
    }
}
