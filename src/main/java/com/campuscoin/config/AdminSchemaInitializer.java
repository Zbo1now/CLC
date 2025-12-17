package com.campuscoin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminSchemaInitializer.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_DISPLAY_NAME = "管理员";
    // admin / 123456
    private static final String DEFAULT_ADMIN_PASSWORD_HASH = "$2a$10$yMi4M7AhiDle9zzik7787u091.y3YZWccl9dnsxkpgi/uMql22JsS";

    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'admin_users'",
                    Integer.class
            );

            if (tableCount == null || tableCount == 0) {
                logger.warn("检测到 admin_users 表不存在，将自动创建（仅首次运行）。");
                jdbcTemplate.execute(
                        "CREATE TABLE IF NOT EXISTS admin_users ("
                                + "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',"
                                + "username VARCHAR(64) NOT NULL UNIQUE COMMENT '管理员登录用户名',"
                                + "password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',"
                                + "display_name VARCHAR(64) NOT NULL DEFAULT '管理员' COMMENT '展示名称',"
                                + "enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用',"
                                + "last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',"
                                + "last_login_ip VARCHAR(64) NULL DEFAULT NULL COMMENT '最后登录IP',"
                                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'"
                                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员账号表'"
                );
            }

            Integer adminCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM admin_users WHERE username = ?",
                    Integer.class,
                    DEFAULT_ADMIN_USERNAME
            );

            if (adminCount == null || adminCount == 0) {
                jdbcTemplate.update(
                        "INSERT INTO admin_users (username, password_hash, display_name, enabled) VALUES (?, ?, ?, 1)",
                        DEFAULT_ADMIN_USERNAME,
                        DEFAULT_ADMIN_PASSWORD_HASH,
                        DEFAULT_ADMIN_DISPLAY_NAME
                );
                logger.warn("已写入预置后台管理员账号：admin/123456（请尽快修改密码）。");
            }
        } catch (Exception ex) {
            // 不阻塞应用启动，但会导致后台登录不可用（避免把真实 SQL 异常暴露给接口调用方）
            logger.error("后台管理员表初始化失败：请检查数据库连接、权限以及是否存在 campuscoin 库。", ex);
        }
    }
}
