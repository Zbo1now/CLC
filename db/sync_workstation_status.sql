-- 同步工位状态：根据当前有效租约更新工位状态

-- 1. 将所有有效租约对应的工位设置为RENTED
UPDATE workstations w
SET status = 'RENTED'
WHERE EXISTS (
    SELECT 1 
    FROM workstation_leases l
    WHERE l.workstation_id = w.id
    AND l.status = 'ACTIVE'
    AND l.start_month <= DATE_FORMAT(CURDATE(), '%Y-%m-01')
    AND l.end_month >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
);

-- 2. 将没有有效租约的工位设置为AVAILABLE
UPDATE workstations w
SET status = 'AVAILABLE'
WHERE NOT EXISTS (
    SELECT 1 
    FROM workstation_leases l
    WHERE l.workstation_id = w.id
    AND l.status = 'ACTIVE'
    AND l.start_month <= DATE_FORMAT(CURDATE(), '%Y-%m-01')
    AND l.end_month >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
)
AND status != 'MAINTENANCE';  -- 保留维护中状态

-- 查看更新后的状态
SELECT 
    w.id,
    w.station_code,
    w.status,
    CASE 
        WHEN EXISTS (
            SELECT 1 
            FROM workstation_leases l
            WHERE l.workstation_id = w.id
            AND l.status = 'ACTIVE'
            AND l.start_month <= DATE_FORMAT(CURDATE(), '%Y-%m-01')
            AND l.end_month >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
        ) THEN '有租约'
        ELSE '无租约'
    END AS lease_status
FROM workstations w
ORDER BY w.station_code;
