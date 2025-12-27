package com.campuscoin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用Spring定时任务调度（@Scheduled）
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
