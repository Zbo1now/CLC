package com.campuscoin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

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
            Map<String, Integer> finish = venueService.autoFinishDueBookings(200);
            int autoCancelledAtEnd = finish != null ? finish.getOrDefault("autoCancelled", 0) : 0;
            int completed = finish != null ? finish.getOrDefault("completed", 0) : 0;

            if (cancelled > 0 || autoCancelledAtEnd > 0 || completed > 0) {
                logger.info("场地预约定时任务: autoCancelAfterStart10m={}, autoCancelAtEnd={}, autoCompleteAtEnd={}",
                        cancelled, autoCancelledAtEnd, completed);
            }
        } catch (Exception e) {
            logger.error("场地预约自动取消任务异常", e);
        }
    }
}
