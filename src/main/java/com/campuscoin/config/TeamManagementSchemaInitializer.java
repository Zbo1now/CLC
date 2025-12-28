package com.campuscoin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 团队管理表结构初始化：
 * 1) teams.enabled：支持账号禁用
 * 2) team_members：支持团队成员管理
 *
 * 说明：该初始化器尽量幂等，仅在字段/表不存在时创建，避免阻塞应用启动。
 */
@Component
public class TeamManagementSchemaInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TeamManagementSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public TeamManagementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureTeamsEnabledColumn();
            ensureTeamMembersTable();
        } catch (Exception ex) {
            logger.error("团队管理表结构初始化失败：请检查数据库连接与权限。", ex);
        }
    }

    private void ensureTeamsEnabledColumn() {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'teams' AND column_name = 'enabled'",
                Integer.class
        );
        if (cnt != null && cnt > 0) {
            return;
        }
        logger.warn("检测到 teams.enabled 字段不存在，将自动添加（仅首次运行）。");
        jdbcTemplate.execute("ALTER TABLE teams ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用'");
    }

    private void ensureTeamMembersTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'team_members'",
                Integer.class
        );
        if (tableCount != null && tableCount > 0) {
            return;
        }

        logger.warn("检测到 team_members 表不存在，将自动创建（仅首次运行）。");
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS team_members ("
                        + "id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',"
                        + "team_id INT NOT NULL COMMENT '所属团队ID',"
                        + "member_name VARCHAR(64) NOT NULL COMMENT '成员姓名',"
                        + "role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：LEADER/MEMBER',"
                        + "phone VARCHAR(32) DEFAULT NULL COMMENT '手机号（可选）',"
                        + "status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',"
                        + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "INDEX idx_team_id (team_id),"
                        + "INDEX idx_team_status (team_id, status),"
                        + "CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表'"
        );
    }
}


