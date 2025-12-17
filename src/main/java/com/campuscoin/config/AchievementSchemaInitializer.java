package com.campuscoin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 成果模块表初始化：避免只执行了 schema.sql 导致成果相关功能/仪表盘统计 500。
 */
@Component
public class AchievementSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AchievementSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public AchievementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'achievement_submissions'",
                    Integer.class
            );

            if (tableCount != null && tableCount > 0) {
                return;
            }

            logger.warn("检测到 achievement_submissions 表不存在，将自动创建（仅首次运行）。");

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS achievement_submissions ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY,"
                            + "team_id INT NOT NULL,"
                            + "category VARCHAR(32) NOT NULL,"
                            + "sub_type VARCHAR(32) NULL,"
                            + "title VARCHAR(255) NOT NULL,"
                            + "proof_url VARCHAR(512) NOT NULL,"
                            + "description VARCHAR(1000) NOT NULL,"
                            + "status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                            + "reward_coins INT NOT NULL DEFAULT 0,"
                            + "reject_reason VARCHAR(255) NULL,"
                            + "reviewed_by VARCHAR(64) NULL,"
                            + "reviewed_at TIMESTAMP NULL,"
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                            + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                            + "INDEX idx_ach_team_time (team_id, created_at),"
                            + "INDEX idx_ach_status_time (status, created_at),"
                            + "CONSTRAINT fk_ach_team FOREIGN KEY (team_id) REFERENCES teams(id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception ex) {
            // 不阻塞启动：避免因为某个表初始化失败导致整个服务启动不了
            logger.error("成果表初始化失败：请检查数据库连接、权限以及 teams 表是否存在。", ex);
        }
    }
}
