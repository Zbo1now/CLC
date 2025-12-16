-- 工位租赁模块（自然月计费）
-- 说明：
-- 1) 工位（workstations）为系统预置资源
-- 2) 租约（workstation_leases）以“月”为单位记录租期：start_month/end_month 均为该月的第一天
-- 3) 通过判断 end_month >= 当月月初 且 status='ACTIVE' 来确定是否在有效租期

USE campuscoin;

CREATE TABLE IF NOT EXISTS workstations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  station_code VARCHAR(32) NOT NULL UNIQUE,
  location VARCHAR(128) NOT NULL,
  monthly_rent INT NOT NULL DEFAULT 50,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_lease_station FOREIGN KEY (workstation_id) REFERENCES workstations(id),
  CONSTRAINT fk_lease_team FOREIGN KEY (team_id) REFERENCES teams(id),
  INDEX idx_lease_station_status (workstation_id, status),
  INDEX idx_lease_team_status (team_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化示例工位（可按需增删）
INSERT IGNORE INTO workstations (station_code, location, monthly_rent) VALUES
('A101', 'A楼 1层 01号', 50),
('A102', 'A楼 1层 02号', 50),
('B202', 'B楼 2层 02号', 50),
('C305', 'C楼 3层 05号', 50);
