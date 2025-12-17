-- ============================================
-- 数据迁移脚本：统一活动管理表
-- 将旧的值班任务和培训活动迁移到统一的activities表
-- ============================================

USE campuscoin;

-- 步骤1：迁移值班任务数据 (duty_tasks -> activities)
INSERT INTO activities (
    activity_name, 
    activity_type, 
    description, 
    location, 
    start_time, 
    end_time, 
    reward_coins, 
    participation_type, 
    max_participants, 
    current_participants,
    status, 
    created_by, 
    created_at, 
    updated_at
)
SELECT 
    task_name AS activity_name,
    'DUTY' AS activity_type,
    IFNULL(task_desc, '') AS description,
    '' AS location,
    start_time,
    end_time,
    reward_coins,
    'TEAM' AS participation_type,
    required_people AS max_participants,
    (SELECT COUNT(*) FROM duty_task_signups WHERE task_id = duty_tasks.id AND signup_status IN ('SIGNED', 'COMPLETED')) AS current_participants,
    CASE 
        WHEN end_time < NOW() THEN 'COMPLETED'
        WHEN start_time <= NOW() AND end_time >= NOW() THEN 'ONGOING'
        ELSE 'NOT_STARTED'
    END AS status,
    'system_migration' AS created_by,
    created_at,
    updated_at
FROM duty_tasks
WHERE NOT EXISTS (
    SELECT 1 FROM activities 
    WHERE activity_type = 'DUTY' 
    AND activity_name = duty_tasks.task_name 
    AND start_time = duty_tasks.start_time
);

-- 步骤2：迁移值班任务报名数据 (duty_task_signups -> activity_participations)
INSERT INTO activity_participations (
    activity_id,
    team_id,
    team_name,
    apply_time,
    status,
    review_status,
    reviewed_by,
    reviewed_at,
    reject_reason,
    coins_rewarded,
    coins_rewarded_at,
    completion_notes,
    created_at,
    updated_at
)
SELECT 
    a.id AS activity_id,
    ds.team_id,
    t.team_name AS team_name,
    ds.signed_at AS apply_time,
    CASE ds.signup_status
        WHEN 'SIGNED' THEN 'APPLIED'
        WHEN 'COMPLETED' THEN 'COMPLETED'
        ELSE 'APPLIED'
    END AS status,
    CASE ds.signup_status
        WHEN 'COMPLETED' THEN 'APPROVED'
        WHEN 'SIGNED' THEN 'PENDING'
        ELSE 'PENDING'
    END AS review_status,
    'system' AS reviewed_by,
    ds.confirmed_at AS reviewed_at,
    NULL AS reject_reason,
    IFNULL(ds.reward_amount, 0) AS coins_rewarded,
    ds.confirmed_at AS coins_rewarded_at,
    '从duty_task_signups迁移' AS completion_notes,
    ds.signed_at AS created_at,
    ds.signed_at AS updated_at
FROM duty_task_signups ds
JOIN duty_tasks dt ON ds.task_id = dt.id
JOIN teams t ON ds.team_id = t.id
JOIN activities a ON a.activity_name = dt.task_name 
    AND a.start_time = dt.start_time 
    AND a.activity_type = 'DUTY'
WHERE NOT EXISTS (
    SELECT 1 FROM activity_participations 
    WHERE activity_id = a.id 
    AND team_id = ds.team_id
);

-- 步骤3：迁移培训活动数据 (training_events -> activities)
INSERT INTO activities (
    activity_name, 
    activity_type, 
    description, 
    location, 
    start_time, 
    end_time, 
    reward_coins, 
    participation_type, 
    max_participants, 
    current_participants,
    status, 
    created_by, 
    created_at, 
    updated_at
)
SELECT 
    event_name AS activity_name,
    'TRAINING' AS activity_type,
    IFNULL(description, '') AS description,
    CASE 
        WHEN location_mode = 'ONLINE' THEN CONCAT('线上：', IFNULL(location_detail, ''))
        WHEN location_mode = 'OFFLINE' THEN IFNULL(location_detail, '')
        ELSE IFNULL(location_detail, '')
    END AS location,
    start_time,
    end_time,
    reward_coins,
    'TEAM' AS participation_type,
    0 AS max_participants,
    (SELECT COUNT(*) FROM training_participations WHERE event_id = training_events.id AND status IN ('APPROVED', 'PENDING')) AS current_participants,
    CASE 
        WHEN publish_status = 'ARCHIVED' THEN 'CANCELLED'
        WHEN end_time < NOW() THEN 'COMPLETED'
        WHEN start_time <= NOW() AND end_time >= NOW() THEN 'ONGOING'
        ELSE 'NOT_STARTED'
    END AS status,
    'system_migration' AS created_by,
    created_at,
    updated_at
FROM training_events
WHERE NOT EXISTS (
    SELECT 1 FROM activities 
    WHERE activity_type = 'TRAINING' 
    AND activity_name = training_events.event_name 
    AND start_time = training_events.start_time
);

-- 步骤4：迁移培训参与数据 (training_participations -> activity_participations)
INSERT INTO activity_participations (
    activity_id,
    team_id,
    team_name,
    apply_time,
    status,
    review_status,
    reviewed_by,
    reviewed_at,
    reject_reason,
    coins_rewarded,
    coins_rewarded_at,
    completion_notes,
    created_at,
    updated_at
)
SELECT 
    a.id AS activity_id,
    tp.team_id,
    t.team_name AS team_name,
    tp.created_at AS apply_time,
    CASE tp.status
        WHEN 'PENDING' THEN 'APPLIED'
        WHEN 'APPROVED' THEN 'COMPLETED'
        WHEN 'REJECTED' THEN 'CANCELLED'
        ELSE 'APPLIED'
    END AS status,
    tp.status AS review_status,
    IFNULL(tp.reviewed_by, 'system') AS reviewed_by,
    tp.reviewed_at,
    tp.reject_reason,
    IFNULL(tp.reward_coins, 0) AS coins_rewarded,
    tp.reviewed_at AS coins_rewarded_at,
    CONCAT('证明材料：', IFNULL(tp.proof_url, ''), '\n备注：', IFNULL(tp.note, '')) AS completion_notes,
    tp.created_at,
    tp.updated_at
FROM training_participations tp
JOIN training_events te ON tp.event_id = te.id
JOIN teams t ON tp.team_id = t.id
JOIN activities a ON a.activity_name = te.event_name 
    AND a.start_time = te.start_time 
    AND a.activity_type = 'TRAINING'
WHERE NOT EXISTS (
    SELECT 1 FROM activity_participations 
    WHERE activity_id = a.id 
    AND team_id = tp.team_id
);

-- 步骤5：验证迁移结果
SELECT '值班任务迁移' AS type, COUNT(*) AS count FROM activities WHERE activity_type = 'DUTY'
UNION ALL
SELECT '培训活动迁移' AS type, COUNT(*) AS count FROM activities WHERE activity_type = 'TRAINING'
UNION ALL
SELECT '值班参与迁移' AS type, COUNT(*) AS count 
FROM activity_participations ap 
JOIN activities a ON ap.activity_id = a.id 
WHERE a.activity_type = 'DUTY'
UNION ALL
SELECT '培训参与迁移' AS type, COUNT(*) AS count 
FROM activity_participations ap 
JOIN activities a ON ap.activity_id = a.id 
WHERE a.activity_type = 'TRAINING';

-- 步骤6：删除旧表（确认迁移无误后执行）
-- 注意：删除前请备份数据！

-- 删除外键约束
ALTER TABLE duty_task_signups DROP FOREIGN KEY fk_duty_task;
ALTER TABLE duty_task_signups DROP FOREIGN KEY fk_duty_team;
ALTER TABLE training_participations DROP FOREIGN KEY fk_training_event;
ALTER TABLE training_participations DROP FOREIGN KEY fk_training_team;

-- 删除旧表
DROP TABLE IF EXISTS duty_task_signups;
DROP TABLE IF EXISTS duty_tasks;
DROP TABLE IF EXISTS training_participations;
DROP TABLE IF EXISTS training_events;

SELECT '数据迁移和表清理完成！' AS message;
