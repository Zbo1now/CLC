package com.campuscoin.service;

import com.campuscoin.dao.TeamDao;
import com.campuscoin.dao.TransactionDao;
import com.campuscoin.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionDao transactionDao;
    private final TeamDao teamDao;

    public TransactionService(TransactionDao transactionDao, TeamDao teamDao) {
        this.transactionDao = transactionDao;
        this.teamDao = teamDao;
    }

    public void record(int teamId, String type, int amount, String description) {
        record(teamId, type, amount, description, null, null);
    }

    /**
     * 写入流水（包含余额快照与业务关联信息）。
     * balance_after 的策略：写入“该笔流水落账后的 teams.balance 快照”。
     */
    public void record(int teamId, String type, int amount, String description, Integer refId, String refType) {
        Transaction txn = new Transaction();
        txn.setTeamId(teamId);
        txn.setTxnType(type);
        txn.setAmount(amount);
        txn.setDescription(description);
        txn.setRefId(refId);
        txn.setRefType(refType);

        Integer balanceAfter = null;
        try {
            balanceAfter = teamDao.getBalance(teamId);
        } catch (Exception e) {
            // 余额快照失败不应阻塞主流程（但会导致后台“操作后余额”为空）
            logger.warn("查询余额快照失败: teamId={}, type={}, amount={}", teamId, type, amount, e);
        }
        txn.setBalanceAfter(balanceAfter);

        transactionDao.create(txn);

        logger.info("流水记录: teamId={}, type={}, amount={}, balanceAfter={}, refType={}, refId={}, desc={}",
                teamId, type, amount, balanceAfter, refType, refId, description);
    }

    public Map<String, Object> listMyTransactions(int teamId, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(100, limit));
        int fetchSize = safeLimit + 1;

        List<Transaction> list = transactionDao.listByTeamPaged(teamId, safeOffset, fetchSize);
        boolean hasMore = list.size() > safeLimit;
        if (hasMore) {
            list = list.subList(0, safeLimit);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("offset", safeOffset);
        data.put("limit", safeLimit);
        data.put("hasMore", hasMore);
        data.put("nextOffset", safeOffset + list.size());
        data.put("transactions", list);

        logger.info("查询流水明细: teamId={}, offset={}, limit={}, returned={}, hasMore={}",
                teamId, safeOffset, safeLimit, list.size(), hasMore);
        return data;
    }
}
