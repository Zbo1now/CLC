-- ===============================
-- 团队管理功能数据库增强脚本
-- 说明：为团队管理功能添加必要的字段和日志表
-- 执行时间：2025-12-17
-- ===============================

USE campuscoin;

-- 1. 为 teams 表添加成员相关字段（如果项目需要存储成员列表）
-- 当前 teams 表已有：id, team_name, password_hash, contact_name, contact_phone, balance, created_at
-- 补充：face_image（人脸识别图片），current_streak（连续打卡天数），last_checkin_date（最后打卡日期）
-- 注意：如果这些字段已经存在，会跳过添加

ALTER TABLE teams 
ADD COLUMN IF NOT EXISTS face_image VARCHAR(255) DEFAULT NULL COMMENT '人脸识别图片URL',
ADD COLUMN IF NOT EXISTS current_streak INT DEFAULT 0 COMMENT '当前连续打卡天数',
ADD COLUMN IF NOT EXISTS last_checkin_date DATE DEFAULT NULL COMMENT '最后打卡日期';

-- 2. 创建管理员操作日志表（记录手动调整虚拟币、重置密码等敏感操作）
CREATE TABLE IF NOT EXISTS admin_operation_logs (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    admin_id INT NOT NULL COMMENT '操作管理员ID',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型：RESET_PASSWORD/ADJUST_BALANCE/EXPORT_REPORT',
    target_type VARCHAR(64) NOT NULL COMMENT '目标对象类型：TEAM',
    target_id INT NOT NULL COMMENT '目标对象ID（如团队ID）',
    operation_detail TEXT COMMENT '操作详情（JSON格式）',
    reason VARCHAR(255) DEFAULT NULL COMMENT '操作原因/备注',
    ip_address VARCHAR(64) DEFAULT NULL COMMENT '操作IP地址',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_admin_id (admin_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- 3. 为常用统计查询创建索引（优化团队列表页的排序和搜索）
-- teams 表索引
ALTER TABLE teams 
ADD INDEX IF NOT EXISTS idx_balance (balance) COMMENT '按余额排序索引',
ADD INDEX IF NOT EXISTS idx_created_at (created_at) COMMENT '按注册时间排序索引',
ADD INDEX IF NOT EXISTS idx_contact_name (contact_name) COMMENT '按联系人搜索索引';

-- transactions 表索引（如果还没有）
ALTER TABLE transactions 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队和时间查询流水',
ADD INDEX IF NOT EXISTS idx_team_type (team_id, txn_type) COMMENT '按团队和类型统计';

-- achievement_submissions 表索引
ALTER TABLE achievement_submissions 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队查询成果';

-- checkins 表索引（如果有单独的打卡表）
-- 注意：根据实际项目，打卡可能记录在 transactions 表中（txn_type='CHECKIN'）
-- 如果有独立的 checkins 表，取消以下注释：
-- ALTER TABLE checkins 
-- ADD INDEX IF NOT EXISTS idx_team_date (team_id, checkin_date) COMMENT '按团队和日期查询打卡';

-- 4. 为资源预约表添加统计索引（计算"本周资源使用次数"）
-- device_bookings
ALTER TABLE device_bookings 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队统计设备预约';

-- venue_bookings
ALTER TABLE venue_bookings 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队统计场地预约';

-- equipment_loans
ALTER TABLE equipment_loans 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队统计器材借用';

-- workstation_leases
ALTER TABLE workstation_leases 
ADD INDEX IF NOT EXISTS idx_team_created (team_id, created_at) COMMENT '按团队统计工位租赁';

-- 5. 数据完整性检查（可选）
-- 确保所有团队的 balance 非空
UPDATE teams SET balance = 0 WHERE balance IS NULL;

-- 确保所有团队的 current_streak 非空
UPDATE teams SET current_streak = 0 WHERE current_streak IS NULL;

-- ===============================
-- 脚本执行完成
-- 说明：已为团队管理功能添加操作日志表和必要索引
-- ===============================
