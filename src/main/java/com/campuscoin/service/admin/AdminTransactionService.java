package com.campuscoin.service.admin;

import com.campuscoin.dao.admin.AdminTransactionDao;
import com.campuscoin.model.admin.AdminTransactionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理系统 - 虚拟币流水查询服务
 */
@Service
public class AdminTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminTransactionService.class);

    private final AdminTransactionDao dao;

    public AdminTransactionService(AdminTransactionDao dao) {
        this.dao = dao;
    }

    /**
     * 分页查询流水记录（支持多条件筛选）
     *
     * @param teamName  团队名称（模糊搜索）
     * @param txnTypes  变动类型列表
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param direction 金额方向（inflow/outflow/all）
     * @param page      页码（从1开始）
     * @param pageSize  每页条数
     * @return 包含 list/total/page/pageSize 的 Map
     */
    public Map<String, Object> listPaged(String teamName,
                                        List<String> txnTypes,
                                        Timestamp startTime,
                                        Timestamp endTime,
                                        String direction,
                                        int page,
                                        int pageSize) {
        logger.info("查询虚拟币流水：teamName={}, txnTypes={}, startTime={}, endTime={}, direction={}, page={}, pageSize={}",
                teamName, txnTypes, startTime, endTime, direction, page, pageSize);

        int offset = (page - 1) * pageSize;

        List<AdminTransactionView> list = dao.listPaged(teamName, txnTypes, startTime, endTime, direction, offset, pageSize);
        int total = dao.countByCondition(teamName, txnTypes, startTime, endTime, direction);

        logger.info("查询虚拟币流水完成：返回 {} 条记录，总计 {} 条", list.size(), total);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
}
