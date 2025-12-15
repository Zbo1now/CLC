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
        String sql = "SELECT id, team_name, password_hash, contact_name, contact_phone, balance, created_at FROM teams WHERE team_name = ?";
        return jdbcTemplate.query(sql, new TeamRowMapper(), teamName).stream().findFirst();
    }

    public boolean createTeam(Team team) {
        String sql = "INSERT INTO teams (team_name, password_hash, contact_name, contact_phone, balance) VALUES (?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql,
                team.getTeamName(),
                team.getPasswordHash(),
            team.getContactName(),
            team.getContactPhone(),
            team.getBalance());
        return rows == 1;
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
            t.setBalance(rs.getInt("balance"));
            t.setCreatedAt(rs.getTimestamp("created_at"));
            return t;
        }
    }
}
