-- 培训/会议活动：管理员预先发布活动 -> 团队参与后提交证明 -> 审核通过后发放虚拟币
-- 当前阶段优先实现用户侧：浏览活动、查看详情、提交参与证明、查看我的申报。
-- 活动发布/审核可先通过 SQL 或未来后台管理系统完成。
USE campuscoin;

-- 培训/会议活动表
CREATE TABLE IF NOT EXISTS training_events (
  id INT AUTO_INCREMENT PRIMARY KEY,
  event_name VARCHAR(255) NOT NULL COMMENT '活动名称，如：AI创业训练营',
  event_type VARCHAR(64) NOT NULL COMMENT '活动类型，如：校内培训/校外会议/讲座/路演/工作会议',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  location_mode VARCHAR(16) NOT NULL DEFAULT 'OFFLINE' COMMENT '地点类型：ONLINE(线上)/OFFLINE(线下)',
  location_detail VARCHAR(255) NOT NULL DEFAULT '' COMMENT '地点详情：线上会议链接/线下地点',
  reward_coins INT NOT NULL COMMENT '奖励虚拟币（通过审核后发放）',
  require_checkin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要签到确认（当前阶段用于展示）',
  description VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '活动描述/说明',
  publish_status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT '发布状态：PUBLISHED/ARCHIVED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_training_time (start_time, end_time),
  INDEX idx_training_status_time (publish_status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 团队参与申报表（上传证明 -> 待审核 -> 通过后发币并写流水）
CREATE TABLE IF NOT EXISTS training_participations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  event_id INT NOT NULL COMMENT '活动ID',
  team_id INT NOT NULL COMMENT '团队ID',
  proof_url VARCHAR(512) NOT NULL COMMENT '参与证明材料URL（合影/笔记等）',
  note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '补充说明（可选）',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING(待审核)/APPROVED(已通过)/REJECTED(已驳回)',
  reward_coins INT NOT NULL DEFAULT 0 COMMENT '实际发放币数（通过时写入，一般等于活动奖励）',
  reject_reason VARCHAR(255) NULL COMMENT '驳回原因',
  reviewed_by VARCHAR(64) NULL COMMENT '审核人（后台）',
  reviewed_at TIMESTAMP NULL COMMENT '审核时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_training_event_team (event_id, team_id) COMMENT '同一团队同一活动只能申报一次',
  INDEX idx_training_team_time (team_id, created_at),
  INDEX idx_training_event_status (event_id, status),
  CONSTRAINT fk_training_event FOREIGN KEY (event_id) REFERENCES training_events(id),
  CONSTRAINT fk_training_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
