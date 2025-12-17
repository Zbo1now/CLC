-- 资源管理表结构优化
-- 日期: 2025-12-17

USE campuscoin;

-- 优化workstations表（工位）
-- 添加区域和状态字段
ALTER TABLE workstations 
ADD COLUMN area VARCHAR(64) COMMENT '区域（如：A区、B区）' AFTER station_code;

ALTER TABLE workstations
ADD COLUMN status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-空闲, RENTED-已租, MAINTENANCE-维护中' AFTER monthly_rent;

-- 优化devices表（设备：3D打印、激光切割等）  
-- 添加型号、位置字段，修改状态说明
ALTER TABLE devices
ADD COLUMN model VARCHAR(64) COMMENT '设备型号' AFTER device_name;

ALTER TABLE devices
ADD COLUMN location VARCHAR(128) COMMENT '设备位置' AFTER device_type;

ALTER TABLE devices
MODIFY COLUMN status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-可用, IN_USE-使用中, BROKEN-故障, MAINTENANCE-维护中';

-- 优化equipments表（器材：相机、摄像机等）
-- 添加型号、数量、可用数量字段
ALTER TABLE equipments
ADD COLUMN model VARCHAR(64) COMMENT '器材型号' AFTER equipment_name;

ALTER TABLE equipments
ADD COLUMN quantity INT DEFAULT 1 COMMENT '总数量' AFTER rate_per_day;

ALTER TABLE equipments
ADD COLUMN available_quantity INT DEFAULT 1 COMMENT '可用数量' AFTER quantity;

ALTER TABLE equipments
MODIFY COLUMN status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-可用, BORROWED-已借出, MAINTENANCE-维护中';

-- 优化venues表（场地：会议室、路演厅等）
-- 添加容量字段
ALTER TABLE venues
ADD COLUMN capacity INT DEFAULT 10 COMMENT '容量（可容纳人数）' AFTER venue_type;

ALTER TABLE venues
MODIFY COLUMN status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-可用, BOOKED-已预订, MAINTENANCE-维护中';
