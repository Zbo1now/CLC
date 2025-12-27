-- 器材借用流程增强（支持库存>1、取消/拒绝/过期、结算退款/补扣）
-- 日期: 2025-12-27
USE campuscoin;

-- 1) equipments 状态统一（兼容历史 ACTIVE）
UPDATE equipments
SET status = 'AVAILABLE'
WHERE status = 'ACTIVE';

-- 2) equipment_loans 增加字段
ALTER TABLE equipment_loans
ADD COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '借用数量' AFTER equipment_id,
ADD COLUMN final_cost INT NOT NULL DEFAULT 0 COMMENT '最终费用（结算后）' AFTER held_cost,
ADD COLUMN approved_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员审核通过时间' AFTER final_cost,
ADD COLUMN rejected_at TIMESTAMP NULL DEFAULT NULL COMMENT '管理员拒绝时间' AFTER approved_at,
ADD COLUMN cancelled_at TIMESTAMP NULL DEFAULT NULL COMMENT '用户取消时间' AFTER rejected_at,
ADD COLUMN expired_at TIMESTAMP NULL DEFAULT NULL COMMENT '超时过期时间' AFTER cancelled_at;

-- 3) 索引（库存占用按日期范围查询会频繁用到）
CREATE INDEX idx_equipment_loans_equipment_dates ON equipment_loans(equipment_id, start_date, end_date);
CREATE INDEX idx_equipment_loans_status_created ON equipment_loans(status, created_at);

-- 说明：
-- status 状态机建议：
-- PENDING(申请中) -> APPROVED(已审核) -> BORROWED(已借出) -> RETURNED(已归还)
-- PENDING/APPROVED 在 start_date 之前允许 CANCELLED(已取消)
-- PENDING/APPROVED 在 start_date 之前允许 REJECTED(已拒绝)
-- PENDING 在创建超过24h且未到 start_date 时自动 EXPIRED(已过期)


