package com.campuscoin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VenueBookingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(VenueBookingScheduler.class);

    private final VenueService venueService;

    public VenueBookingScheduler(VenueService venueService) {
        this.venueService = venueService;
    }

    // 每分钟扫描一次：预约开始后 10 分钟内未确认 => 自动取消（不退款）
    @Scheduled(fixedDelay = 60_000)
    public void autoCancel() {
        try {
            int cancelled = venueService.autoCancelExpiredBooked(200);
            if (cancelled > 0) {
                logger.info("场地预约自动取消任务: cancelled={}", cancelled);
            }
        } catch (Exception e) {
            logger.error("场地预约自动取消任务异常", e);
        }
    }
}
