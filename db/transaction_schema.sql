-- 账户流水表（用于首页“近期流水摘要”等展示）
USE campuscoin;

CREATE TABLE IF NOT EXISTS transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  team_id INT NOT NULL,
  txn_type VARCHAR(32) NOT NULL,
  amount INT NOT NULL,
  description VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_txn_team_time (team_id, created_at),
  CONSTRAINT fk_txn_team FOREIGN KEY (team_id) REFERENCES teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
