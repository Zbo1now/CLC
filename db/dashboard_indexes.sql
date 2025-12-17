-- =========================
-- 仪表盘统计优化索引（可选）
-- 说明：仪表盘会按时间范围聚合流水、成果、值班、培训、预约等数据。
-- 如数据量较大，建议执行本脚本提升查询性能。
-- =========================

USE campuscoin;

-- 流水：按 created_at + txn_type 聚合
CREATE INDEX idx_txn_time_type ON transactions (created_at, txn_type);

-- 培训：审核通过按 reviewed_at 聚合；行为统计按 created_at 聚合
CREATE INDEX idx_training_reviewed_status ON training_participations (status, reviewed_at);
CREATE INDEX idx_training_created_at ON training_participations (created_at);

-- 器材借用：热门资源统计按 created_at 聚合
CREATE INDEX idx_equipment_loans_created_at ON equipment_loans (created_at);

-- 工位租约：热门资源统计按 created_at 聚合
CREATE INDEX idx_workstation_leases_created_at ON workstation_leases (created_at);

-- 场地预约：热门资源统计按 created_at 聚合
CREATE INDEX idx_venue_bookings_created_at ON venue_bookings (created_at);

-- 设备预约：热门资源统计按 created_at 聚合
CREATE INDEX idx_device_bookings_created_at ON device_bookings (created_at);
