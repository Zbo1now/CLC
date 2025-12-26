-- 添加 location 字段到 activities 表
ALTER TABLE activities ADD COLUMN location VARCHAR(255) DEFAULT NULL COMMENT '活动地点';
