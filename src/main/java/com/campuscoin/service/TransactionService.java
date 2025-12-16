package com.campuscoin.service;

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

    public TransactionService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    public void record(int teamId, String type, int amount, String description) {
        Transaction txn = new Transaction();
        txn.setTeamId(teamId);
        txn.setTxnType(type);
        txn.setAmount(amount);
        txn.setDescription(description);
        transactionDao.create(txn);

        logger.info("流水记录: teamId={}, type={}, amount={}, desc={}", teamId, type, amount, description);
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
