-- 设备租用模块（按分钟计费，费率以“币/小时”配置）
-- 说明：
-- 1) devices 为系统预置设备（可手动维护 status：IDLE / MAINTENANCE）
-- 2) device_bookings 记录预约与实际使用：RESERVED -> IN_USE -> FINISHED
-- 3) 扣费在 finish 时按实际使用时长计算：cost = ceil(minutes * rate_per_hour / 60)

USE campuscoin;

CREATE TABLE IF NOT EXISTS devices (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  device_name VARCHAR(64) NOT NULL UNIQUE COMMENT '设备名称（唯一）',
  device_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '设备类型（如 3D_PRINTER / LASER_CUTTER）',
  rate_per_hour INT NOT NULL DEFAULT 20 COMMENT '计费费率：币/小时（示例：3D打印机20，激光切割机30）',
  status VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT '设备维护状态：IDLE(可用)/MAINTENANCE(维护中)；运行态由系统计算',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备资源表（可维护费率与维护状态）';

CREATE TABLE IF NOT EXISTS device_bookings (
  id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  device_id INT NOT NULL COMMENT '设备ID（关联 devices.id）',
  team_id INT NOT NULL COMMENT '团队ID（关联 teams.id）',
  start_at DATETIME NOT NULL COMMENT '预约开始时间',
  end_at DATETIME NOT NULL COMMENT '预约结束时间',
  actual_start_at DATETIME NULL COMMENT '实际开始时间（开始使用时写入）',
  actual_end_at DATETIME NULL COMMENT '实际结束时间（结束结算时写入）',
  status VARCHAR(16) NOT NULL DEFAULT 'RESERVED' COMMENT '状态：RESERVED(已预约)/IN_USE(使用中)/FINISHED(已结束)',
  billed_cost INT NOT NULL DEFAULT 0 COMMENT '本次结算扣费（币，结束结算时写入）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  CONSTRAINT fk_booking_device FOREIGN KEY (device_id) REFERENCES devices(id),
  CONSTRAINT fk_booking_team FOREIGN KEY (team_id) REFERENCES teams(id),
  INDEX idx_booking_device_status_time (device_id, status, start_at, end_at),
  INDEX idx_booking_team_status_time (team_id, status, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备预约/使用记录表（按实际使用时长扣费）';

-- 初始化示例设备（可按需增删）
INSERT IGNORE INTO devices (device_name, device_type, rate_per_hour, status) VALUES
('3D 打印机 A', '3D_PRINTER', 20, 'IDLE'),
('激光切割机 B', 'LASER_CUTTER', 30, 'IDLE');
