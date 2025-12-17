package com.campuscoin.dao.admin;

import com.campuscoin.model.admin.AdminTransactionView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;
import java.util.List;

/**
 * 后台管理系统 - 虚拟币流水查询 DAO
 */
@Mapper
public interface AdminTransactionDao {

    /**
     * 分页查询流水（支持多条件筛选）
     *
     * @param teamName    团队名称（模糊搜索，可为空）
     * @param txnTypes    变动类型列表（可为空）
     * @param startTime   开始时间（可为空）
     * @param endTime     结束时间（可为空）
     * @param direction   金额方向：inflow(收入)/outflow(支出)/all(全部)
     * @param offset      分页偏移
     * @param limit       每页条数
     * @return 流水记录列表
     */
    @Select({
            "<script>",
            "SELECT ",
            "  t.id, t.team_id AS teamId, tm.team_name AS teamName, ",
            "  t.txn_type AS txnType, t.amount, t.balance_after AS balanceAfter, ",
            "  t.description, t.ref_id AS refId, t.ref_type AS refType, t.created_at AS createdAt ",
            "FROM transactions t ",
            "LEFT JOIN teams tm ON t.team_id = tm.id ",
            "WHERE 1=1 ",
            "<if test='teamName != null and teamName != \"\"'>",
            "  AND tm.team_name LIKE CONCAT('%', #{teamName}, '%') ",
            "</if>",
            "<if test='txnTypes != null and txnTypes.size() > 0'>",
            "  AND t.txn_type IN ",
            "  <foreach collection='txnTypes' item='type' open='(' separator=',' close=')'>",
            "    #{type}",
            "  </foreach>",
            "</if>",
            "<if test='startTime != null'>",
            "  AND t.created_at &gt;= #{startTime} ",
            "</if>",
            "<if test='endTime != null'>",
            "  AND t.created_at &lt; #{endTime} ",
            "</if>",
            "<if test='direction == \"inflow\"'>",
            "  AND t.amount &gt; 0 ",
            "</if>",
            "<if test='direction == \"outflow\"'>",
            "  AND t.amount &lt; 0 ",
            "</if>",
            "ORDER BY t.created_at DESC ",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<AdminTransactionView> listPaged(@Param("teamName") String teamName,
                                         @Param("txnTypes") List<String> txnTypes,
                                         @Param("startTime") Timestamp startTime,
                                         @Param("endTime") Timestamp endTime,
                                         @Param("direction") String direction,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /**
     * 统计总数（同样的筛选条件）
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) ",
            "FROM transactions t ",
            "LEFT JOIN teams tm ON t.team_id = tm.id ",
            "WHERE 1=1 ",
            "<if test='teamName != null and teamName != \"\"'>",
            "  AND tm.team_name LIKE CONCAT('%', #{teamName}, '%') ",
            "</if>",
            "<if test='txnTypes != null and txnTypes.size() > 0'>",
            "  AND t.txn_type IN ",
            "  <foreach collection='txnTypes' item='type' open='(' separator=',' close=')'>",
            "    #{type}",
            "  </foreach>",
            "</if>",
            "<if test='startTime != null'>",
            "  AND t.created_at &gt;= #{startTime} ",
            "</if>",
            "<if test='endTime != null'>",
            "  AND t.created_at &lt; #{endTime} ",
            "</if>",
            "<if test='direction == \"inflow\"'>",
            "  AND t.amount &gt; 0 ",
            "</if>",
            "<if test='direction == \"outflow\"'>",
            "  AND t.amount &lt; 0 ",
            "</if>",
            "</script>"
    })
    int countByCondition(@Param("teamName") String teamName,
                        @Param("txnTypes") List<String> txnTypes,
                        @Param("startTime") Timestamp startTime,
                        @Param("endTime") Timestamp endTime,
                        @Param("direction") String direction);
}
