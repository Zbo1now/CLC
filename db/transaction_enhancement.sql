-- 虚拟币流水表功能增强
-- 说明：为后台管理系统"虚拟币流水"功能补充字段
USE campuscoin;

-- 增加操作后余额快照字段（用于快速展示每笔流水后的余额，避免实时计算）
ALTER TABLE transactions 
ADD COLUMN balance_after INT NULL DEFAULT NULL COMMENT '操作后余额快照' 
AFTER amount;

-- 增加关联业务ID字段（用于跳转详情，如成果ID、预约ID等）
ALTER TABLE transactions 
ADD COLUMN ref_id INT NULL DEFAULT NULL COMMENT '关联业务ID（如成果ID、设备预约ID等）' 
AFTER description;

-- 增加关联业务类型字段（用于判断跳转目标）
ALTER TABLE transactions 
ADD COLUMN ref_type VARCHAR(32) NULL DEFAULT NULL COMMENT '关联业务类型（如achievement、device_booking等）' 
AFTER ref_id;

-- 为常用查询条件增加索引
CREATE INDEX idx_txn_type_time ON transactions(txn_type, created_at);
CREATE INDEX idx_txn_created ON transactions(created_at DESC);

-- 注意：现有流水数据的 balance_after 为 NULL，可在后台统计时跳过或按需补充
