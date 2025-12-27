-- 器材借用模块（最新版：支持库存>1、取消/拒绝/过期、结算退款/补扣）
-- 说明：
-- 1) equipments 存放器材基础信息与库存（quantity / available_quantity）与费率（币/天）
-- 2) equipment_loans 存放借用申请与状态流转（PENDING -> APPROVED -> BORROWED -> RETURNED）
--    终止态：CANCELLED / REJECTED / EXPIRED
--
-- 注意：本文件用于开发环境初始化；生产请使用迁移脚本，避免 DROP TABLE 误删数据。

DROP TABLE IF EXISTS equipment_loans;
DROP TABLE IF EXISTS equipments;

CREATE TABLE equipments (
  id INT PRIMARY KEY AUTO_INCREMENT COMMENT '器材ID',
  equipment_name VARCHAR(100) NOT NULL COMMENT '器材名称',
  model VARCHAR(64) DEFAULT NULL COMMENT '器材型号',
  equipment_type VARCHAR(100) NOT NULL COMMENT '器材类型/分类',
  rate_per_day INT NOT NULL COMMENT '费率（币/天）',
  quantity INT NOT NULL DEFAULT 1 COMMENT '总数量',
  available_quantity INT NOT NULL DEFAULT 1 COMMENT '可用数量（当前未借出）',
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/MAINTENANCE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可借用器材';

CREATE TABLE equipment_loans (
  id INT PRIMARY KEY AUTO_INCREMENT COMMENT '借用记录ID',
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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_equipment_status_dates (equipment_id, status, start_date, end_date),
  INDEX idx_team_status_dates (team_id, status, start_date, end_date),
  INDEX idx_equipment_loans_equipment_dates (equipment_id, start_date, end_date),
  INDEX idx_equipment_loans_status_created (status, created_at),
  CONSTRAINT fk_equipment_loans_equipment FOREIGN KEY (equipment_id) REFERENCES equipments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器材借用申请/借出记录';

-- 示例数据
INSERT INTO equipments (equipment_name, model, equipment_type, rate_per_day, quantity, available_quantity, status)
VALUES
  ('Canon 单反', '5D Mark IV', '相机', 10, 3, 3, 'AVAILABLE'),
  ('Sony 摄像机', 'FX3', '摄像设备', 12, 2, 2, 'AVAILABLE');
