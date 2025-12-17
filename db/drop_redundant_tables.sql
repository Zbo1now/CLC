-- ============================================
-- 删除冗余表脚本
-- 删除旧的值班任务和培训活动表
-- ============================================

USE campuscoin;

-- 步骤1：删除外键约束
ALTER TABLE duty_task_signups DROP FOREIGN KEY fk_duty_task;
ALTER TABLE duty_task_signups DROP FOREIGN KEY fk_duty_team;

ALTER TABLE training_participations DROP FOREIGN KEY fk_training_event;
ALTER TABLE training_participations DROP FOREIGN KEY fk_training_team;

-- 步骤2：删除旧表
DROP TABLE IF EXISTS duty_task_signups;
DROP TABLE IF EXISTS duty_tasks;
DROP TABLE IF EXISTS training_participations;
DROP TABLE IF EXISTS training_events;

-- 步骤3：验证删除结果
SHOW TABLES;

SELECT '冗余表删除完成！' AS message;
