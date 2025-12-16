-- 成果提交表（团队提交 -> 管理员审核 -> 通过后发放虚拟币）
USE campuscoin;

CREATE TABLE IF NOT EXISTS achievement_submissions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL,
  category VARCHAR(32) NOT NULL,
  sub_type VARCHAR(32) NULL,
  title VARCHAR(255) NOT NULL,
  proof_url VARCHAR(512) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  reward_coins INT NOT NULL DEFAULT 0,
  reject_reason VARCHAR(255) NULL,
  reviewed_by VARCHAR(64) NULL,
  reviewed_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ach_team_time (team_id, created_at),
  INDEX idx_ach_status_time (status, created_at),
  CONSTRAINT fk_ach_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
