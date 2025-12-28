-- ===============================
-- 团队管理增强：账号禁用 + 成员表
-- 执行时间：2025-12-28
-- 说明：可重复执行（幂等），建议在生产环境先备份再执行
-- ===============================

USE campuscoin;

-- 1) teams 增加 enabled 字段（MySQL 8.0.29+ 支持 IF NOT EXISTS）
ALTER TABLE teams
ADD COLUMN IF NOT EXISTS enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用';

-- 2) 创建团队成员表
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

-- ===============================
-- 脚本结束
-- ===============================


