-- 器材借用模块
-- 说明：
-- 1) equipments 存放可借用器材与费率（币/天）
-- 2) equipment_loans 存放借用申请与状态流转（PENDING -> BORROWED -> RETURNED）

DROP TABLE IF EXISTS equipment_loans;
DROP TABLE IF EXISTS equipments;

CREATE TABLE equipments (
  id INT PRIMARY KEY AUTO_INCREMENT COMMENT '器材ID',
  equipment_name VARCHAR(100) NOT NULL COMMENT '器材名称',
  equipment_type VARCHAR(100) NOT NULL COMMENT '器材类型/分类',
  rate_per_day INT NOT NULL COMMENT '费率（币/天）',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/MAINTENANCE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可借用器材';

CREATE TABLE equipment_loans (
  id INT PRIMARY KEY AUTO_INCREMENT COMMENT '借用记录ID',
  team_id INT NOT NULL COMMENT '团队ID',
  equipment_id INT NOT NULL COMMENT '器材ID',
  start_date DATE NOT NULL COMMENT '借用开始日期（含）',
  end_date DATE NOT NULL COMMENT '借用结束日期（含）',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(申请中)/BORROWED(已借出)/RETURNED(已归还)',
  held_cost INT NOT NULL DEFAULT 0 COMMENT '预扣费用（币）',
  borrowed_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员确认借出时间',
  returned_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员确认归还时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_equipment_status_dates (equipment_id, status, start_date, end_date),
  INDEX idx_team_status_dates (team_id, status, start_date, end_date),
  CONSTRAINT fk_equipment_loans_equipment FOREIGN KEY (equipment_id) REFERENCES equipments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器材借用申请/借出记录';

-- 示例数据
INSERT INTO equipments (equipment_name, equipment_type, rate_per_day, status)
VALUES
  ('Canon 单反', '相机', 10, 'ACTIVE'),
  ('Sony 摄像机', '摄像设备', 12, 'ACTIVE');
