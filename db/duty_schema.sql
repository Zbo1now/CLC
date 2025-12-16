-- 值班任务：发布任务 -> 团队报名 -> 结束后待确认 -> 管理员确认后发币
-- 说明：当前阶段优先实现用户侧（列表/报名）。任务发布可先通过 SQL 手动插入。
USE campuscoin;

-- 值班任务表
CREATE TABLE IF NOT EXISTS duty_tasks (
  id INT AUTO_INCREMENT PRIMARY KEY,
  task_name VARCHAR(255) NOT NULL COMMENT '任务名称，如：周一前台值班',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  required_people INT NOT NULL COMMENT '所需人数',
  reward_coins INT NOT NULL COMMENT '奖励虚拟币（每人）',
  task_desc VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '任务描述',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_duty_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 值班任务报名表
CREATE TABLE IF NOT EXISTS duty_task_signups (
  id INT AUTO_INCREMENT PRIMARY KEY,
  task_id INT NOT NULL COMMENT '值班任务ID',
  team_id INT NOT NULL COMMENT '报名团队ID',
  signup_status VARCHAR(24) NOT NULL DEFAULT 'SIGNED' COMMENT '报名状态：SIGNED(已报名)/COMPLETED(已确认发币)',
  signed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  confirmed_at TIMESTAMP NULL COMMENT '管理员确认时间',
  reward_amount INT NOT NULL DEFAULT 0 COMMENT '实际发币数量（每人），确认时写入',
  UNIQUE KEY uk_duty_task_team (task_id, team_id) COMMENT '同一任务每个团队只能报名一次',
  INDEX idx_duty_task_status (task_id, signup_status),
  INDEX idx_duty_team_time (team_id, signed_at),
  CONSTRAINT fk_duty_task FOREIGN KEY (task_id) REFERENCES duty_tasks(id),
  CONSTRAINT fk_duty_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
