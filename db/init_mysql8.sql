-- ============================================================
-- CampusCoin 一体化初始化脚本（MySQL 8）
-- 目标：在一台全新 MySQL 实例中“一次执行”完成建库、建表、索引、必要的初始数据
--
-- 建议用法：
--   mysql -u root -p < init_mysql8.sql
--
-- 说明：
-- - 本脚本不包含 DROP TABLE / DROP DATABASE（避免误删生产数据）
-- - 表结构以当前后端代码（DAO/Model）与 db 目录脚本综合后的“最终形态”为准
-- ============================================================

CREATE DATABASE IF NOT EXISTS campuscoin
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE campuscoin;

-- =========================
-- 1) 团队与成员
-- =========================

CREATE TABLE IF NOT EXISTS teams (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_name VARCHAR(128) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  contact_phone VARCHAR(32) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用/0禁用',
  face_image VARCHAR(255) DEFAULT NULL COMMENT '人脸识别图片URL',
  current_streak INT NOT NULL DEFAULT 0 COMMENT '当前连续打卡天数',
  last_checkin_date DATE DEFAULT NULL COMMENT '最后打卡日期',
  balance INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_team_balance (balance),
  INDEX idx_team_created_at (created_at),
  INDEX idx_team_contact_name (contact_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队表';

CREATE TABLE IF NOT EXISTS team_members (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  team_id INT NOT NULL COMMENT '所属团队ID',
  member_name VARCHAR(64) NOT NULL COMMENT '成员姓名',
  role VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：LEADER/MEMBER',
  phone VARCHAR(32) DEFAULT NULL COMMENT '手机号（可选）',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_team_members_team_id (team_id),
  INDEX idx_team_members_team_status (team_id, status),
  CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队成员表';

-- =========================
-- 2) 管理员
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

-- 预置管理员账号：admin / 123456
-- 说明：这里写入的是 BCrypt 哈希（非明文）。
INSERT INTO admin_users (username, password_hash, display_name, enabled)
SELECT 'admin', '$2a$10$yMi4M7AhiDle9zzik7787u091.y3YZWccl9dnsxkpgi/uMql22JsS', '管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM admin_users WHERE username = 'admin');

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
  INDEX idx_admin_operation_admin_id (admin_id),
  INDEX idx_admin_operation_target (target_type, target_id),
  INDEX idx_admin_operation_created_at (created_at),
  CONSTRAINT fk_admin_operation_admin FOREIGN KEY (admin_id) REFERENCES admin_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- =========================
-- 3) 虚拟币流水（transactions）
-- =========================

CREATE TABLE IF NOT EXISTS transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL,
  txn_type VARCHAR(32) NOT NULL,
  amount INT NOT NULL,
  balance_after INT NULL DEFAULT NULL COMMENT '操作后余额快照',
  description VARCHAR(255) NOT NULL,
  ref_id INT NULL DEFAULT NULL COMMENT '关联业务ID（如成果ID、预约ID等）',
  ref_type VARCHAR(32) NULL DEFAULT NULL COMMENT '关联业务类型（如achievement、device_booking等）',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_txn_team_time (team_id, created_at),
  INDEX idx_txn_type_time (txn_type, created_at),
  INDEX idx_txn_created_at (created_at),
  CONSTRAINT fk_txn_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟币流水表';

-- =========================
-- 4) 打卡记录（checkins）
-- =========================

CREATE TABLE IF NOT EXISTS checkins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL,
  checkin_date DATE NOT NULL,
  coins_earned INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_checkin_team_date (team_id, checkin_date),
  INDEX idx_checkin_date (checkin_date),
  CONSTRAINT fk_checkin_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团队打卡记录表';

-- =========================
-- 5) 成果提交（achievement_submissions）
-- =========================

CREATE TABLE IF NOT EXISTS achievement_submissions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL,
  category VARCHAR(32) NOT NULL,
  sub_type VARCHAR(32) NULL,
  title VARCHAR(255) NOT NULL,
  proof_url VARCHAR(512) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  reward_coins INT NOT NULL DEFAULT 0,
  reject_reason VARCHAR(255) NULL,
  reviewed_by VARCHAR(64) NULL,
  reviewed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ach_team_time (team_id, created_at),
  INDEX idx_ach_status_time (status, created_at),
  CONSTRAINT fk_ach_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成果提交表';

-- =========================
-- 6) 统一活动（activities / activity_participations）
-- =========================

CREATE TABLE IF NOT EXISTS activities (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
  activity_name VARCHAR(200) NOT NULL COMMENT '活动名称',
  activity_type VARCHAR(50) NOT NULL COMMENT '活动类型：DUTY/TRAINING',
  description TEXT COMMENT '活动描述',
  location VARCHAR(200) DEFAULT NULL COMMENT '活动地点',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  reward_coins INT DEFAULT 0 COMMENT '虚拟币奖励值',
  participation_type VARCHAR(50) DEFAULT 'MANUAL_REVIEW' COMMENT '参与方式：MANUAL_REVIEW（人工审核）',
  max_participants INT DEFAULT 0 COMMENT '最大参与团队数（0表示不限制）',
  current_participants INT DEFAULT 0 COMMENT '当前参与团队数',
  status VARCHAR(50) DEFAULT 'NOT_STARTED' COMMENT '活动状态：NOT_STARTED/ONGOING/FINISHED/CANCELLED',
  created_by VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_activity_type (activity_type),
  INDEX idx_activity_status (status),
  INDEX idx_activity_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动信息表';

CREATE TABLE IF NOT EXISTS activity_participations (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '参与记录ID',
  activity_id INT NOT NULL COMMENT '活动ID',
  team_id INT NOT NULL COMMENT '团队ID',
  team_name VARCHAR(100) DEFAULT NULL COMMENT '团队名称（冗余字段，便于查询）',
  apply_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  status VARCHAR(50) DEFAULT 'PENDING' COMMENT '参与状态：PENDING/APPLIED/APPROVED/REJECTED/COMPLETED/CANCELLED/IN_PROGRESS',
  review_status VARCHAR(50) DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  reviewed_by VARCHAR(100) DEFAULT NULL COMMENT '审核人',
  reviewed_at TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
  reject_reason TEXT DEFAULT NULL COMMENT '驳回原因',
  coins_rewarded INT DEFAULT 0 COMMENT '已发放币值',
  coins_rewarded_at TIMESTAMP NULL DEFAULT NULL COMMENT '发币时间',
  completion_notes TEXT DEFAULT NULL COMMENT '完成备注/证明材料',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_activity_team (activity_id, team_id),
  INDEX idx_participation_status (status),
  INDEX idx_participation_review (review_status),
  INDEX idx_participation_activity (activity_id),
  INDEX idx_participation_team (team_id),
  CONSTRAINT fk_participation_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
  CONSTRAINT fk_participation_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录表';

-- =========================
-- 7) 工位（workstations / workstation_leases）
-- =========================

CREATE TABLE IF NOT EXISTS workstations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  station_code VARCHAR(32) NOT NULL UNIQUE,
  area VARCHAR(64) DEFAULT NULL COMMENT '区域（如：A区、B区）',
  location VARCHAR(128) NOT NULL,
  monthly_rent INT NOT NULL DEFAULT 50,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RENTED/MAINTENANCE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_workstation_status (status),
  INDEX idx_workstation_area (area)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位资源表';

CREATE TABLE IF NOT EXISTS workstation_leases (
  id INT AUTO_INCREMENT PRIMARY KEY,
  workstation_id INT NOT NULL,
  team_id INT NOT NULL,
  start_month DATE NOT NULL,
  end_month DATE NOT NULL,
  monthly_rent INT NOT NULL,
  total_cost INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  released_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_lease_station FOREIGN KEY (workstation_id) REFERENCES workstations(id),
  CONSTRAINT fk_lease_team FOREIGN KEY (team_id) REFERENCES teams(id),
  INDEX idx_lease_station_status (workstation_id, status),
  INDEX idx_lease_team_status (team_id, status),
  INDEX idx_workstation_leases_team_created (team_id, created_at),
  INDEX idx_workstation_leases_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位租约表（自然月计费）';

-- =========================
-- 8) 设备（devices / device_bookings）
-- =========================

CREATE TABLE IF NOT EXISTS devices (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  device_name VARCHAR(64) NOT NULL UNIQUE COMMENT '设备名称（唯一）',
  model VARCHAR(64) DEFAULT NULL COMMENT '设备型号',
  device_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '设备类型（如 3D_PRINTER / LASER_CUTTER）',
  location VARCHAR(128) DEFAULT NULL COMMENT '设备位置',
  rate_per_hour INT NOT NULL DEFAULT 20 COMMENT '计费费率：币/小时',
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '维护状态：AVAILABLE/IN_USE/BROKEN/MAINTENANCE（历史兼容：IDLE=AVAILABLE）',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备资源表';

CREATE TABLE IF NOT EXISTS device_bookings (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  device_id INT NOT NULL COMMENT '设备ID（关联 devices.id）',
  team_id INT NOT NULL COMMENT '团队ID（关联 teams.id）',
  start_at DATETIME NOT NULL COMMENT '预约开始时间',
  end_at DATETIME NOT NULL COMMENT '预约结束时间',
  actual_start_at DATETIME NULL COMMENT '实际开始时间',
  actual_end_at DATETIME NULL COMMENT '实际结束时间',
  status VARCHAR(16) NOT NULL DEFAULT 'RESERVED' COMMENT '状态：RESERVED/IN_USE/FINISHED',
  billed_cost INT NOT NULL DEFAULT 0 COMMENT '本次结算扣费（或预扣值，按业务实现写入）',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_booking_device FOREIGN KEY (device_id) REFERENCES devices(id),
  CONSTRAINT fk_booking_team FOREIGN KEY (team_id) REFERENCES teams(id),
  INDEX idx_booking_device_status_time (device_id, status, start_at, end_at),
  INDEX idx_booking_team_status_time (team_id, status, start_at, end_at),
  INDEX idx_device_bookings_team_created (team_id, created_at),
  INDEX idx_device_bookings_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备预约/使用记录表';

-- =========================
-- 9) 器材（equipments / equipment_loans）
-- =========================

CREATE TABLE IF NOT EXISTS equipments (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '器材ID',
  equipment_name VARCHAR(100) NOT NULL COMMENT '器材名称',
  model VARCHAR(64) DEFAULT NULL COMMENT '器材型号',
  equipment_type VARCHAR(100) NOT NULL COMMENT '器材类型/分类',
  rate_per_day INT NOT NULL COMMENT '费率（币/天）',
  quantity INT NOT NULL DEFAULT 1 COMMENT '总数量',
  available_quantity INT NOT NULL DEFAULT 1 COMMENT '可用数量（当前未借出）',
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/MAINTENANCE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_equipment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可借用器材';

CREATE TABLE IF NOT EXISTS equipment_loans (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '借用记录ID',
  team_id INT NOT NULL COMMENT '团队ID',
  equipment_id INT NOT NULL COMMENT '器材ID',
  quantity INT NOT NULL DEFAULT 1 COMMENT '借用数量',
  start_date DATE NOT NULL COMMENT '借用开始日期（含）',
  end_date DATE NOT NULL COMMENT '借用结束日期（含）',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/BORROWED/RETURNED/CANCELLED/REJECTED/EXPIRED',
  held_cost INT NOT NULL DEFAULT 0 COMMENT '预扣费用（币）',
  final_cost INT NOT NULL DEFAULT 0 COMMENT '最终费用（结算后）',
  approved_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员审核通过时间',
  rejected_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员拒绝时间',
  cancelled_at TIMESTAMP NULL DEFAULT NULL COMMENT '用户取消时间',
  expired_at TIMESTAMP NULL DEFAULT NULL COMMENT '超时过期时间',
  borrowed_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员确认借出时间',
  returned_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员确认归还时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_equipment_status_dates (equipment_id, status, start_date, end_date),
  INDEX idx_team_status_dates (team_id, status, start_date, end_date),
  INDEX idx_equipment_loans_equipment_dates (equipment_id, start_date, end_date),
  INDEX idx_equipment_loans_status_created (status, created_at),
  INDEX idx_equipment_loans_team_created (team_id, created_at),
  INDEX idx_equipment_loans_created_at (created_at),
  CONSTRAINT fk_equipment_loans_equipment FOREIGN KEY (equipment_id) REFERENCES equipments(id),
  CONSTRAINT fk_equipment_loans_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器材借用申请/借出记录';

-- =========================
-- 10) 场地（venues / venue_bookings）
-- =========================

CREATE TABLE IF NOT EXISTS venues (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  venue_name VARCHAR(64) NOT NULL COMMENT '场地名称',
  venue_type VARCHAR(32) NOT NULL COMMENT '场地类型',
  capacity INT NOT NULL DEFAULT 10 COMMENT '容量（可容纳人数）',
  rate_per_hour INT NOT NULL DEFAULT 0 COMMENT '每小时费用（虚拟币）',
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/MAINTENANCE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_venue_name (venue_name),
  INDEX idx_venue_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地信息表（短租计费）';

CREATE TABLE IF NOT EXISTS venue_bookings (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  team_id INT NOT NULL COMMENT '团队ID',
  venue_id INT NOT NULL COMMENT '场地ID',
  start_time DATETIME NOT NULL COMMENT '预约开始时间',
  end_time DATETIME NOT NULL COMMENT '预约结束时间',
  status VARCHAR(16) NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED/IN_USE/AUTO_CANCELLED/CANCELLED/COMPLETED',
  held_cost INT NOT NULL DEFAULT 0 COMMENT '预扣虚拟币（按小时数计算）',
  confirmed_at DATETIME NULL COMMENT '确认使用时间',
  cancelled_at DATETIME NULL COMMENT '取消时间',
  finished_at DATETIME NULL COMMENT '结束时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_team_status (team_id, status),
  INDEX idx_venue_time (venue_id, start_time, end_time),
  INDEX idx_status_start (status, start_time),
  INDEX idx_venue_bookings_team_created (team_id, created_at),
  INDEX idx_venue_bookings_created_at (created_at),
  CONSTRAINT fk_venue_booking_venue FOREIGN KEY (venue_id) REFERENCES venues(id),
  CONSTRAINT fk_venue_booking_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地短租预约表';

-- =========================
-- 11) 系统配置（system_configs）
-- =========================

CREATE TABLE IF NOT EXISTS system_configs (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键（唯一标识，如 reward.checkin）',
  config_value TEXT NOT NULL COMMENT '配置值（支持数字、字符串、JSON等）',
  value_type VARCHAR(20) DEFAULT 'NUMBER' COMMENT '值类型：NUMBER/STRING/BOOLEAN/JSON',
  category VARCHAR(50) NOT NULL COMMENT '分类：REWARD/ACTIVITY/RESOURCE/SYSTEM',
  display_name VARCHAR(100) DEFAULT NULL COMMENT '显示名称（中文名，用于界面展示）',
  description VARCHAR(255) DEFAULT NULL COMMENT '配置说明（详细描述配置项的用途）',
  unit VARCHAR(20) DEFAULT NULL COMMENT '单位（如：币、天、小时、%）',
  min_value INT DEFAULT NULL COMMENT '最小值限制（用于数值型配置）',
  max_value INT DEFAULT NULL COMMENT '最大值限制（用于数值型配置）',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序（同类别配置的显示顺序）',
  is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0=禁用，1=启用）',
  updated_by VARCHAR(50) DEFAULT NULL COMMENT '最后修改人（管理员用户名）',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_category (category, sort_order),
  INDEX idx_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始化 system_configs（幂等：按 config_key 唯一键避免重复）
INSERT INTO system_configs (config_key, config_value, value_type, category, display_name, description, unit, min_value, max_value, sort_order)
VALUES
  ('reward.checkin', '50', 'NUMBER', 'REWARD', '每日打卡奖励', '团队成员每天打卡可获得的虚拟币奖励', '币', 10, 200, 1),
  ('reward.achievement.patent', '800', 'NUMBER', 'REWARD', '专利成果奖励', '提交专利申请成果通过审核后的奖励', '币', 100, 2000, 2),
  ('reward.achievement.paper', '600', 'NUMBER', 'REWARD', '论文成果奖励', '发表学术论文通过审核后的奖励', '币', 100, 1500, 3),
  ('reward.achievement.competition', '500', 'NUMBER', 'REWARD', '竞赛成果奖励', '参加竞赛获奖通过审核后的奖励', '币', 100, 1500, 4),
  ('reward.achievement.project', '700', 'NUMBER', 'REWARD', '科研项目奖励', '科研项目成果通过审核后的奖励', '币', 100, 2000, 5),
  ('reward.achievement.other', '300', 'NUMBER', 'REWARD', '其他成果奖励', '其他类型成果通过审核后的奖励', '币', 50, 1000, 6),
  ('reward.duty.default', '30', 'NUMBER', 'REWARD', '值班活动默认奖励', '创建值班活动时的默认奖励金额（可在创建时调整）', '币', 10, 200, 7),
  ('reward.training.default', '40', 'NUMBER', 'REWARD', '培训活动默认奖励', '创建培训活动时的默认奖励金额（可在创建时调整）', '币', 10, 200, 8),

  ('activity.signup.max_participants', '50', 'NUMBER', 'ACTIVITY', '活动最大参与人数', '单个活动允许报名的最大人数限制', '人', 5, 200, 1),
  ('activity.cancel.deadline_hours', '2', 'NUMBER', 'ACTIVITY', '取消报名截止时间', '活动开始前多少小时不允许取消报名', '小时', 1, 48, 2),
  ('activity.checkin.advance_minutes', '15', 'NUMBER', 'ACTIVITY', '签到提前时间', '活动开始前多少分钟可以签到', '分钟', 5, 60, 3),
  ('activity.checkin.late_minutes', '30', 'NUMBER', 'ACTIVITY', '签到延迟时间', '活动开始后多少分钟内仍可签到', '分钟', 10, 120, 4),

  ('resource.workstation.default_rate', '20', 'NUMBER', 'RESOURCE', '工位默认租金', '新建工位时的默认每日租金（可在创建时调整）', '币/天', 5, 100, 1),
  ('resource.device.default_rate', '15', 'NUMBER', 'RESOURCE', '设备默认租金', '新建设备时的默认每小时租金（可在创建时调整）', '币/小时', 5, 100, 2),
  ('resource.equipment.default_deposit', '100', 'NUMBER', 'RESOURCE', '器材默认押金', '新建器材时的默认借用押金（可在创建时调整）', '币', 20, 500, 3),
  ('resource.venue.default_rate', '50', 'NUMBER', 'RESOURCE', '场地默认费用', '新建场地时的默认预订费用（可在创建时调整）', '币/场', 20, 500, 4),
  ('resource.booking.max_duration_hours', '8', 'NUMBER', 'RESOURCE', '资源预订最长时长', '单次预订资源的最长时长限制', '小时', 1, 24, 5),
  ('resource.booking.advance_days', '7', 'NUMBER', 'RESOURCE', '资源预订提前天数', '最多可以提前多少天预订资源', '天', 1, 30, 6),

  ('system.team.initial_balance', '1000', 'NUMBER', 'SYSTEM', '团队初始余额', '新注册团队的初始虚拟币余额', '币', 500, 5000, 1),
  ('system.balance.min_threshold', '0', 'NUMBER', 'SYSTEM', '余额最低阈值', '团队虚拟币余额不能低于此值（防止负债）', '币', 0, 100, 2),
  ('system.password.reset_default', 'campus123', 'STRING', 'SYSTEM', '密码重置默认值', '管理员重置团队密码时的默认密码', '', NULL, NULL, 3),
  ('system.file.max_size_mb', '10', 'NUMBER', 'SYSTEM', '文件上传大小限制', '成果附件等文件上传的最大大小', 'MB', 1, 50, 4)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  value_type = VALUES(value_type),
  category = VALUES(category),
  display_name = VALUES(display_name),
  description = VALUES(description),
  unit = VALUES(unit),
  min_value = VALUES(min_value),
  max_value = VALUES(max_value),
  sort_order = VALUES(sort_order),
  is_enabled = 1;

-- =========================
-- 12) 可选：示例资源数据（需要可取消注释）
-- =========================

-- INSERT IGNORE INTO workstations (station_code, area, location, monthly_rent, status) VALUES
-- ('A101', 'A区', 'A楼 1层 01号', 50, 'AVAILABLE'),
-- ('A102', 'A区', 'A楼 1层 02号', 50, 'AVAILABLE'),
-- ('B202', 'B区', 'B楼 2层 02号', 50, 'AVAILABLE'),
-- ('C305', 'C区', 'C楼 3层 05号', 50, 'AVAILABLE');

-- INSERT IGNORE INTO devices (device_name, model, device_type, location, rate_per_hour, status) VALUES
-- ('3D 打印机 A', 'ProJet MJP 2500', '3D_PRINTER', '创客实验室-A区', 20, 'AVAILABLE'),
-- ('激光切割机 B', 'Thunder Laser Nova', 'LASER_CUTTER', '创客实验室-B区', 30, 'AVAILABLE');

-- INSERT IGNORE INTO equipments (equipment_name, model, equipment_type, rate_per_day, quantity, available_quantity, status) VALUES
-- ('Canon 单反', '5D Mark IV', '相机', 10, 3, 3, 'AVAILABLE'),
-- ('Sony 摄像机', 'FX3', '摄像设备', 12, 2, 2, 'AVAILABLE');

-- INSERT IGNORE INTO venues (venue_name, venue_type, capacity, rate_per_hour, status) VALUES
-- ('A1 会议室', '会议室', 10, 40, 'AVAILABLE'),
-- ('路演厅 B', '路演厅', 60, 80, 'AVAILABLE'),
-- ('A2 会议室', '会议室', 35, 40, 'MAINTENANCE');


