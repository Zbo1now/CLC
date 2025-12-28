CREATE DATABASE IF NOT EXISTS campuscoin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campuscoin;

CREATE TABLE IF NOT EXISTS teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    team_name VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用',
    balance INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 团队成员表（团队中心成员管理）
CREATE TABLE IF NOT EXISTS team_members (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    team_id INT NOT NULL COMMENT '所属团队ID',
    member_name VARCHAR(64) NOT NULL COMMENT '成员姓名',
    role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：LEADER/MEMBER',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号（可选）',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_team_id (team_id),
    INDEX idx_team_status (team_id, status),
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表';

-- =========================
-- 管理员账号体系（后台管理）
-- 说明：首次部署时预置一个管理员账号（admin / 123456）。
-- 安全：密码不存明文，存 BCrypt 哈希。
-- =========================

CREATE TABLE IF NOT EXISTS admin_users (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '管理员登录用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    display_name VARCHAR(64) NOT NULL DEFAULT '管理员' COMMENT '展示名称',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用',
    last_login_at TIMESTAMP NULL DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) NULL DEFAULT NULL COMMENT '最后登录IP',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员账号表';

-- 预置管理员账号（admin / 123456）
-- 注意：password_hash 需要使用 BCrypt 生成。下方值会在后续步骤中写入真实 hash。
INSERT INTO admin_users (username, password_hash, display_name, enabled)
SELECT 'admin', '$2a$10$yMi4M7AhiDle9zzik7787u091.y3YZWccl9dnsxkpgi/uMql22JsS', '管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM admin_users WHERE username = 'admin');
