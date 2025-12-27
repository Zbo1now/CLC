package com.campuscoin.task;

import com.campuscoin.service.EquipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 器材借用申请超时自动过期（24小时）
 * 规则：仅处理 PENDING 且 created_at <= now-24h 且未到 start_date 的记录，过期后自动退款并释放占用
 */
@Component
public class EquipmentLoanExpireTask {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentLoanExpireTask.class);

    @Resource
    private EquipmentService equipmentService;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void expire() {
        LocalDateTime now = LocalDateTime.now();
        Timestamp deadline = Timestamp.valueOf(now.minusHours(24));
        int processed = equipmentService.autoExpirePendingLoans(LocalDate.now(), deadline);
        if (processed > 0) {
            logger.info("器材借用申请自动过期: processed={}", processed);
        }
    }
}


