package com.campuscoin.dao;

import com.campuscoin.model.Transaction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TransactionDao {

    @Insert("INSERT INTO transactions (team_id, txn_type, amount, balance_after, description, ref_id, ref_type) " +
            "VALUES (#{teamId}, #{txnType}, #{amount}, #{balanceAfter}, #{description}, #{refId}, #{refType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int create(Transaction txn);

    @Select("SELECT id, team_id AS teamId, txn_type AS txnType, amount, balance_after AS balanceAfter, description, " +
            "ref_id AS refId, ref_type AS refType, created_at AS createdAt " +
            "FROM transactions WHERE team_id = #{teamId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Transaction> listRecentByTeam(@Param("teamId") int teamId, @Param("limit") int limit);

        @Select("SELECT id, team_id AS teamId, txn_type AS txnType, amount, balance_after AS balanceAfter, description, " +
            "ref_id AS refId, ref_type AS refType, created_at AS createdAt " +
            "FROM transactions WHERE team_id = #{teamId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
        List<Transaction> listByTeamPaged(@Param("teamId") int teamId, @Param("offset") int offset, @Param("limit") int limit);
}
