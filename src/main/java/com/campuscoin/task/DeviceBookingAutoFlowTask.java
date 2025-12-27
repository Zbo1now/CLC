package com.campuscoin.task;

import com.campuscoin.dao.DeviceBookingDao;
import com.campuscoin.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class DeviceBookingAutoFlowTask {
    private static final Logger logger = LoggerFactory.getLogger(DeviceBookingAutoFlowTask.class);

    @Resource
    private DeviceBookingDao deviceBookingDao;

    @Resource
    private DeviceService deviceService;

    /**
     * 定时自动流转设备预约状态：
     * <ul>
     *   <li>每分钟执行一次（0 * * * * ?）</li>
     *   <li>将所有到点的RESERVED预约自动转为IN_USE，actual_start_at设为start_at</li>
     *   <li>将所有到点的IN_USE预约自动转为FINISHED，actual_end_at设为end_at</li>
     *   <li>无需人工干预，保证预约状态与时间强一致</li>
     *   <li>如需暂停自动流转，可注释掉@Scheduled注解</li>
     * </ul>
     * 注意：需配合@EnableScheduling生效
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void autoFlow() {
        LocalDateTime now = LocalDateTime.now();
        // 1. RESERVED 到点自动转 IN_USE
        int reservedToInUse = deviceBookingDao.autoStart(now);
        // 2. IN_USE 到点自动结束并结算（方案B：只退不补，自动结束也需要走结算逻辑，避免漏扣/重复扣）
        DeviceService.AutoFinishResult finishResult = deviceService.autoFinishDueBookings(now);

        if (reservedToInUse > 0 || (finishResult != null && finishResult.getProcessed() > 0)) {
            logger.info("设备预约自动流转: RESERVED->IN_USE:{}，AUTO_FINISH processed={}, refundedCount={}, refundedTotal={}, failedIds={}",
                    reservedToInUse,
                    finishResult != null ? finishResult.getProcessed() : 0,
                    finishResult != null ? finishResult.getRefundedCount() : 0,
                    finishResult != null ? finishResult.getRefundedTotal() : 0,
                    finishResult != null ? finishResult.getFailedBookingIds() : null
            );
        }
    }
}
