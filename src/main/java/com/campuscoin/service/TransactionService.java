package com.campuscoin.service;

import com.campuscoin.dao.TransactionDao;
import com.campuscoin.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
}
