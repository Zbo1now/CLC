-- =========================
-- 管理员账号体系（后台管理）
-- 增量脚本：如果你已经执行过 db/schema.sql（旧版），只需要再执行本脚本即可。
-- 说明：首次部署时预置一个管理员账号（admin / 123456）。
-- 安全：密码不存明文，存 BCrypt 哈希。
-- =========================

USE campuscoin;

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
INSERT INTO admin_users (username, password_hash, display_name, enabled)
SELECT 'admin', '$2a$10$yMi4M7AhiDle9zzik7787u091.y3YZWccl9dnsxkpgi/uMql22JsS', '管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM admin_users WHERE username = 'admin');
