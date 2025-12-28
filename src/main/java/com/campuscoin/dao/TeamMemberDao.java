package com.campuscoin.dao;

import com.campuscoin.model.TeamMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TeamMemberDao {

    @Select("SELECT id, team_id AS teamId, member_name AS memberName, role, phone, status, created_at AS createdAt " +
            "FROM team_members WHERE team_id = #{teamId} ORDER BY id DESC")
    List<TeamMember> listByTeam(@Param("teamId") int teamId);

    @Insert("INSERT INTO team_members (team_id, member_name, role, phone, status) " +
            "VALUES (#{teamId}, #{memberName}, #{role}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeamMember member);
}


