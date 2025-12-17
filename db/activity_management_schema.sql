-- ============================================
-- 活动管理模块数据库表结构
-- 包含：活动表、参与记录表
-- ============================================

-- 活动表
CREATE TABLE IF NOT EXISTS activities (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    activity_name VARCHAR(200) NOT NULL COMMENT '活动名称',
    activity_type VARCHAR(50) NOT NULL COMMENT '活动类型：DUTY（值班任务）、TRAINING（培训/会议）',
    description TEXT COMMENT '活动描述',
    location VARCHAR(200) COMMENT '活动地点',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reward_coins INT DEFAULT 0 COMMENT '虚拟币奖励值',
    participation_type VARCHAR(50) DEFAULT 'MANUAL_REVIEW' COMMENT '参与方式：MANUAL_REVIEW（人工审核）',
    max_participants INT DEFAULT 0 COMMENT '最大参与团队数（0表示不限制）',
    current_participants INT DEFAULT 0 COMMENT '当前参与团队数',
    status VARCHAR(50) DEFAULT 'NOT_STARTED' COMMENT '活动状态：NOT_STARTED（未开始）、ONGOING（进行中）、FINISHED（已结束）、CANCELLED（已取消）',
    created_by VARCHAR(100) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_activity_type (activity_type),
    INDEX idx_activity_status (status),
    INDEX idx_activity_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动信息表';

-- 活动参与记录表
CREATE TABLE IF NOT EXISTS activity_participations (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '参与记录ID',
    activity_id INT NOT NULL COMMENT '活动ID',
    team_id INT NOT NULL COMMENT '团队ID',
    team_name VARCHAR(100) COMMENT '团队名称（冗余字段，便于查询）',
    apply_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT '参与状态：PENDING（待审核）、APPROVED（已通过）、REJECTED（已驳回）、COMPLETED（已完成）',
    review_status VARCHAR(50) DEFAULT 'PENDING' COMMENT '审核状态：PENDING（待审核）、APPROVED（已通过）、REJECTED（已驳回）',
    reviewed_by VARCHAR(100) COMMENT '审核人',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间',
    reject_reason TEXT COMMENT '驳回原因',
    coins_rewarded INT DEFAULT 0 COMMENT '已发放币值',
    coins_rewarded_at TIMESTAMP NULL COMMENT '发币时间',
    completion_notes TEXT COMMENT '完成备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    UNIQUE KEY uk_activity_team (activity_id, team_id) COMMENT '同一活动每个团队只能参与一次',
    INDEX idx_participation_status (status),
    INDEX idx_participation_review (review_status),
    INDEX idx_participation_activity (activity_id),
    INDEX idx_participation_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录表';
