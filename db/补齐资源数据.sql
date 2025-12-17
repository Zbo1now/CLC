-- 补齐资源管理表的缺失数据
-- 日期: 2025-12-17

USE campuscoin;

-- ========== 1. 补齐工位表（workstations）缺失数据 ==========

-- 补充area字段（根据station_code推断区域）
UPDATE workstations 
SET area = CASE 
    WHEN station_code LIKE 'A%' THEN 'A区'
    WHEN station_code LIKE 'B%' THEN 'B区'
    WHEN station_code LIKE 'C%' THEN 'C区'
    WHEN station_code LIKE 'D%' THEN 'D区'
    ELSE '未分区'
END
WHERE area IS NULL OR area = '';

-- 确保所有工位都有状态（如果没有租约则为AVAILABLE）
UPDATE workstations w
SET status = CASE 
    WHEN EXISTS (
        SELECT 1 
        FROM workstation_leases l
        WHERE l.workstation_id = w.id
        AND l.status = 'ACTIVE'
        AND l.start_month <= DATE_FORMAT(CURDATE(), '%Y-%m-01')
        AND l.end_month >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
    ) THEN 'RENTED'
    ELSE 'AVAILABLE'
END
WHERE status IS NULL OR status = '';

-- ========== 2. 补齐设备表（devices）缺失数据 ==========

-- 补充model字段（根据device_name推断型号）
UPDATE devices 
SET model = CASE 
    WHEN device_name LIKE '%3D 打印机%' THEN 'ProJet MJP 2500'
    WHEN device_name LIKE '%激光切割机%' THEN 'Thunder Laser Nova'
    WHEN device_name LIKE '%打印%' THEN 'Generic-3DP-01'
    WHEN device_name LIKE '%切割%' THEN 'Generic-LC-01'
    ELSE 'Standard Model'
END
WHERE model IS NULL OR model = '';

-- 补充location字段（根据device_type推断位置）
UPDATE devices 
SET location = CASE 
    WHEN device_type = '3D_PRINTER' OR device_name LIKE '%3D%' OR device_name LIKE '%打印%' THEN '创客实验室-A区'
    WHEN device_type = 'LASER_CUTTER' OR device_name LIKE '%激光%' OR device_name LIKE '%切割%' THEN '创客实验室-B区'
    WHEN device_type = 'CNC' THEN '机械加工区'
    ELSE '设备存放区'
END
WHERE location IS NULL OR location = '';

-- 确保所有设备都有合理的状态
UPDATE devices 
SET status = CASE 
    WHEN status IS NULL OR status = '' THEN 'AVAILABLE'
    WHEN status = 'IDLE' THEN 'AVAILABLE'
    WHEN status NOT IN ('AVAILABLE', 'IN_USE', 'BROKEN', 'MAINTENANCE') THEN 'AVAILABLE'
    ELSE status
END;

-- ========== 3. 补齐器材表（equipments）缺失数据 ==========

-- 补充model字段（根据equipment_name推断型号）
UPDATE equipments 
SET model = CASE 
    WHEN equipment_name LIKE '%Canon%' THEN 'EOS 5D Mark IV'
    WHEN equipment_name LIKE '%Sony%' THEN 'PXW-Z150'
    WHEN equipment_name LIKE '%Nikon%' THEN 'D850'
    WHEN equipment_name LIKE '%单反%' THEN 'DSLR-Standard'
    WHEN equipment_name LIKE '%摄像%' THEN 'Camcorder-Pro'
    WHEN equipment_name LIKE '%相机%' THEN 'Camera-Standard'
    ELSE 'Standard Model'
END
WHERE model IS NULL OR model = '';

-- 补充quantity字段（默认为1）
UPDATE equipments 
SET quantity = 1
WHERE quantity IS NULL OR quantity = 0;

-- 补充available_quantity字段
-- 如果有借用中的记录，需要减去；否则等于总数量
UPDATE equipments e
SET available_quantity = (
    SELECT e.quantity - COALESCE(SUM(CASE WHEN l.status = 'BORROWED' THEN 1 ELSE 0 END), 0)
    FROM equipment_loans l
    WHERE l.equipment_id = e.id
    AND l.status = 'BORROWED'
)
WHERE available_quantity IS NULL;

-- 如果没有借用记录，available_quantity应该等于quantity
UPDATE equipments 
SET available_quantity = quantity
WHERE available_quantity IS NULL OR available_quantity < 0;

-- 确保状态正确（ACTIVE改为AVAILABLE）
UPDATE equipments 
SET status = CASE 
    WHEN status = 'ACTIVE' THEN 'AVAILABLE'
    WHEN status IS NULL OR status = '' THEN 'AVAILABLE'
    WHEN status NOT IN ('AVAILABLE', 'BORROWED', 'MAINTENANCE') THEN 'AVAILABLE'
    ELSE status
END;

-- ========== 4. 补齐场地表（venues）缺失数据 ==========

-- 补充capacity字段（根据venue_type推断容量）
UPDATE venues 
SET capacity = CASE 
    WHEN venue_type LIKE '%会议室%' THEN 15
    WHEN venue_type LIKE '%路演厅%' OR venue_type LIKE '%大厅%' THEN 50
    WHEN venue_type LIKE '%小会议室%' THEN 8
    WHEN venue_type LIKE '%培训室%' THEN 30
    ELSE 20
END
WHERE capacity IS NULL OR capacity = 0;

-- 确保状态正确（ACTIVE改为AVAILABLE）
UPDATE venues 
SET status = CASE 
    WHEN status = 'ACTIVE' THEN 'AVAILABLE'
    WHEN status IS NULL OR status = '' THEN 'AVAILABLE'
    WHEN status NOT IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE') THEN 'AVAILABLE'
    ELSE status
END;

-- ========== 验证数据完整性 ==========

-- 检查工位数据完整性
SELECT 
    '工位数据检查' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN area IS NULL OR area = '' THEN 1 ELSE 0 END) AS missing_area,
    SUM(CASE WHEN status IS NULL OR status = '' THEN 1 ELSE 0 END) AS missing_status
FROM workstations;

-- 检查设备数据完整性
SELECT 
    '设备数据检查' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN model IS NULL OR model = '' THEN 1 ELSE 0 END) AS missing_model,
    SUM(CASE WHEN location IS NULL OR location = '' THEN 1 ELSE 0 END) AS missing_location,
    SUM(CASE WHEN status IS NULL OR status = '' THEN 1 ELSE 0 END) AS missing_status
FROM devices;

-- 检查器材数据完整性
SELECT 
    '器材数据检查' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN model IS NULL OR model = '' THEN 1 ELSE 0 END) AS missing_model,
    SUM(CASE WHEN quantity IS NULL OR quantity = 0 THEN 1 ELSE 0 END) AS missing_quantity,
    SUM(CASE WHEN available_quantity IS NULL THEN 1 ELSE 0 END) AS missing_available_quantity,
    SUM(CASE WHEN status IS NULL OR status = '' THEN 1 ELSE 0 END) AS missing_status
FROM equipments;

-- 检查场地数据完整性
SELECT 
    '场地数据检查' AS table_name,
    COUNT(*) AS total_records,
    SUM(CASE WHEN capacity IS NULL OR capacity = 0 THEN 1 ELSE 0 END) AS missing_capacity,
    SUM(CASE WHEN status IS NULL OR status = '' THEN 1 ELSE 0 END) AS missing_status
FROM venues;

-- 显示所有资源的最终状态
SELECT 'workstations' AS resource_type, id, station_code AS name, area, location, status FROM workstations
UNION ALL
SELECT 'devices', id, device_name, model, location, status FROM devices
UNION ALL
SELECT 'equipments', id, equipment_name, model, CONCAT('数量:', quantity, '/可用:', available_quantity), status FROM equipments
UNION ALL
SELECT 'venues', id, venue_name, venue_type, CONCAT('容量:', capacity), status FROM venues
ORDER BY resource_type, id;
