-- 系统配置表建表语句
-- 用于动态调整虚拟币奖励、资源定价等系统参数，无需重启服务即可生效

USE campuscoin;

-- 创建系统配置表
DROP TABLE IF EXISTS system_configs;
CREATE TABLE system_configs (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键（唯一标识，如 reward.checkin）',
    config_value TEXT NOT NULL COMMENT '配置值（支持数字、字符串、JSON等）',
    value_type VARCHAR(20) DEFAULT 'NUMBER' COMMENT '值类型：NUMBER/STRING/BOOLEAN/JSON',
    category VARCHAR(50) NOT NULL COMMENT '分类：REWARD（激励）/ACTIVITY（活动）/RESOURCE（资源）/SYSTEM（系统）',
    display_name VARCHAR(100) COMMENT '显示名称（中文名，用于界面展示）',
    description VARCHAR(255) COMMENT '配置说明（详细描述配置项的用途）',
    unit VARCHAR(20) COMMENT '单位（如：币、天、小时、%）',
    min_value INT COMMENT '最小值限制（用于数值型配置）',
    max_value INT COMMENT '最大值限制（用于数值型配置）',
    sort_order INT DEFAULT 0 COMMENT '排序（同类别配置的显示顺序）',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用（0=禁用，1=启用）',
    updated_by VARCHAR(50) COMMENT '最后修改人（管理员用户名）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表 - 动态管理虚拟币激励和资源定价';

-- 创建索引
CREATE INDEX idx_category ON system_configs(category, sort_order);
CREATE INDEX idx_enabled ON system_configs(is_enabled);

-- ========== 初始化配置数据 ==========

-- 【激励配置】REWARD
INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, description, unit, min_value, max_value, sort_order) VALUES
('reward.checkin', '50', 'NUMBER', 'REWARD', '每日打卡奖励', '团队成员每天打卡可获得的虚拟币奖励', '币', 10, 200, 1),
('reward.achievement.patent', '800', 'NUMBER', 'REWARD', '专利成果奖励', '提交专利申请成果通过审核后的奖励', '币', 100, 2000, 2),
('reward.achievement.paper', '600', 'NUMBER', 'REWARD', '论文成果奖励', '发表学术论文通过审核后的奖励', '币', 100, 1500, 3),
('reward.achievement.competition', '500', 'NUMBER', 'REWARD', '竞赛成果奖励', '参加竞赛获奖通过审核后的奖励', '币', 100, 1500, 4),
('reward.achievement.project', '700', 'NUMBER', 'REWARD', '科研项目奖励', '科研项目成果通过审核后的奖励', '币', 100, 2000, 5),
('reward.achievement.other', '300', 'NUMBER', 'REWARD', '其他成果奖励', '其他类型成果通过审核后的奖励', '币', 50, 1000, 6),
('reward.duty.default', '30', 'NUMBER', 'REWARD', '值班活动默认奖励', '创建值班活动时的默认奖励金额（可在创建时调整）', '币', 10, 200, 7),
('reward.training.default', '40', 'NUMBER', 'REWARD', '培训活动默认奖励', '创建培训活动时的默认奖励金额（可在创建时调整）', '币', 10, 200, 8);

-- 【活动配置】ACTIVITY
INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, description, unit, min_value, max_value, sort_order) VALUES
('activity.signup.max_participants', '50', 'NUMBER', 'ACTIVITY', '活动最大参与人数', '单个活动允许报名的最大人数限制', '人', 5, 200, 1),
('activity.cancel.deadline_hours', '2', 'NUMBER', 'ACTIVITY', '取消报名截止时间', '活动开始前多少小时不允许取消报名', '小时', 1, 48, 2),
('activity.checkin.advance_minutes', '15', 'NUMBER', 'ACTIVITY', '签到提前时间', '活动开始前多少分钟可以签到', '分钟', 5, 60, 3),
('activity.checkin.late_minutes', '30', 'NUMBER', 'ACTIVITY', '签到延迟时间', '活动开始后多少分钟内仍可签到', '分钟', 10, 120, 4);

-- 【资源配置】RESOURCE  
INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, description, unit, min_value, max_value, sort_order) VALUES
('resource.workstation.default_rate', '20', 'NUMBER', 'RESOURCE', '工位默认租金', '新建工位时的默认每日租金（可在创建时调整）', '币/天', 5, 100, 1),
('resource.device.default_rate', '15', 'NUMBER', 'RESOURCE', '设备默认租金', '新建设备时的默认每小时租金（可在创建时调整）', '币/小时', 5, 100, 2),
('resource.equipment.default_deposit', '100', 'NUMBER', 'RESOURCE', '器材默认押金', '新建器材时的默认借用押金（可在创建时调整）', '币', 20, 500, 3),
('resource.venue.default_rate', '50', 'NUMBER', 'RESOURCE', '场地默认费用', '新建场地时的默认预订费用（可在创建时调整）', '币/场', 20, 500, 4),
('resource.booking.max_duration_hours', '8', 'NUMBER', 'RESOURCE', '资源预订最长时长', '单次预订资源的最长时长限制', '小时', 1, 24, 5),
('resource.booking.advance_days', '7', 'NUMBER', 'RESOURCE', '资源预订提前天数', '最多可以提前多少天预订资源', '天', 1, 30, 6);

-- 【系统配置】SYSTEM
INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, description, unit, min_value, max_value, sort_order) VALUES
('system.team.initial_balance', '1000', 'NUMBER', 'SYSTEM', '团队初始余额', '新注册团队的初始虚拟币余额', '币', 500, 5000, 1),
('system.balance.min_threshold', '0', 'NUMBER', 'SYSTEM', '余额最低阈值', '团队虚拟币余额不能低于此值（防止负债）', '币', 0, 100, 2),
('system.password.reset_default', 'campus123', 'STRING', 'SYSTEM', '密码重置默认值', '管理员重置团队密码时的默认密码', '', NULL, NULL, 3),
('system.file.max_size_mb', '10', 'NUMBER', 'SYSTEM', '文件上传大小限制', '成果附件等文件上传的最大大小', 'MB', 1, 50, 4);

-- 查询初始化结果
SELECT 
    category AS '配置分类',
    COUNT(*) AS '配置项数量'
FROM system_configs
GROUP BY category
ORDER BY FIELD(category, 'REWARD', 'ACTIVITY', 'RESOURCE', 'SYSTEM');

SELECT '配置表创建成功！' AS '提示', COUNT(*) AS '总配置项数' FROM system_configs;
