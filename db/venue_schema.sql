-- 场地短租（会议室、路演厅等）表结构
-- 说明：
-- 1) venues：场地基础信息与计费规则（按小时）
-- 2) venue_bookings：团队预约记录（预扣币写入流水，自动取消不退款）

DROP TABLE IF EXISTS venue_bookings;
DROP TABLE IF EXISTS venues;

-- 场地表
CREATE TABLE venues (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    venue_name VARCHAR(64) NOT NULL COMMENT '场地名称（如 A1 会议室、路演厅 B）',
    venue_type VARCHAR(32) NOT NULL COMMENT '场地类型（如 会议室/路演厅）',
    rate_per_hour INT NOT NULL DEFAULT 0 COMMENT '每小时费用（虚拟币）',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '场地状态（ACTIVE=可用，MAINTENANCE=维护中）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_venue_name (venue_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地信息表（短租计费）';

-- 预约表
CREATE TABLE venue_bookings (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    team_id INT NOT NULL COMMENT '团队ID（关联 teams.id）',
    venue_id INT NOT NULL COMMENT '场地ID（关联 venues.id）',

    start_time DATETIME NOT NULL COMMENT '预约开始时间（本地时间）',
    end_time DATETIME NOT NULL COMMENT '预约结束时间（本地时间）',

    status VARCHAR(16) NOT NULL DEFAULT 'BOOKED' COMMENT '预约状态（BOOKED=已预约，IN_USE=使用中，AUTO_CANCELLED=自动取消，CANCELLED=已取消，COMPLETED=已结束）',
    held_cost INT NOT NULL DEFAULT 0 COMMENT '预扣虚拟币（按小时数计算）',

    confirmed_at DATETIME NULL COMMENT '确认使用时间',
    cancelled_at DATETIME NULL COMMENT '取消时间',
    finished_at DATETIME NULL COMMENT '结束时间',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_team_status (team_id, status),
    KEY idx_venue_time (venue_id, start_time, end_time),
    KEY idx_status_start (status, start_time),

    CONSTRAINT fk_venue_booking_venue FOREIGN KEY (venue_id) REFERENCES venues(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地短租预约表（预扣币，不退款）';

-- 示例数据
INSERT INTO venues (venue_name, venue_type, rate_per_hour, status) VALUES
('A1 会议室', '会议室', 40, 'ACTIVE'),
('路演厅 B', '路演厅', 80, 'ACTIVE'),
('A2 会议室', '会议室', 35, 'MAINTENANCE');
