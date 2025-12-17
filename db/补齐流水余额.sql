-- 补齐流水表的 balance_after 字段
-- 策略：按团队分组，按时间顺序计算每笔流水后的余额快照

USE campuscoin;

-- 1. 创建临时表存储计算结果
DROP TEMPORARY TABLE IF EXISTS temp_transaction_balance;
CREATE TEMPORARY TABLE temp_transaction_balance (
    txn_id INT PRIMARY KEY,
    balance_after INT
);

-- 2. 为每个团队按时间顺序计算余额
-- 方案：使用变量+会话级计算（适用于MySQL 5.7+）

-- 2.1 为团队1计算
SET @running_balance = (SELECT balance FROM teams WHERE id = 1);
INSERT INTO temp_transaction_balance (txn_id, balance_after)
SELECT 
    id,
    @running_balance := @running_balance - amount AS balance_after
FROM transactions
WHERE team_id = 1
ORDER BY created_at, id;

-- 2.2 为团队2计算
SET @running_balance = (SELECT balance FROM teams WHERE id = 2);
INSERT INTO temp_transaction_balance (txn_id, balance_after)
SELECT 
    id,
    @running_balance := @running_balance - amount AS balance_after
FROM transactions
WHERE team_id = 2
ORDER BY created_at, id;

-- 2.3 为团队3计算
SET @running_balance = (SELECT balance FROM teams WHERE id = 3);
INSERT INTO temp_transaction_balance (txn_id, balance_after)
SELECT 
    id,
    @running_balance := @running_balance - amount AS balance_after
FROM transactions
WHERE team_id = 3
ORDER BY created_at, id;

-- 2.4 为团队4计算
SET @running_balance = (SELECT balance FROM teams WHERE id = 4);
INSERT INTO temp_transaction_balance (txn_id, balance_after)
SELECT 
    id,
    @running_balance := @running_balance - amount AS balance_after
FROM transactions
WHERE team_id = 4
ORDER BY created_at, id;

-- 3. 更新 transactions 表
UPDATE transactions t
INNER JOIN temp_transaction_balance tmp ON t.id = tmp.txn_id
SET t.balance_after = tmp.balance_after;

-- 4. 验证补齐结果
SELECT 
    '补齐统计' AS info,
    COUNT(*) AS total_transactions,
    SUM(CASE WHEN balance_after IS NOT NULL THEN 1 ELSE 0 END) AS filled_count,
    SUM(CASE WHEN balance_after IS NULL THEN 1 ELSE 0 END) AS null_count
FROM transactions;

-- 5. 检查每个团队最后一笔流水的余额是否与当前余额一致
SELECT 
    'balance 一致性检查' AS check_type,
    t.id AS team_id,
    t.team_name,
    t.balance AS current_balance,
    last_tx.balance_after AS last_transaction_balance,
    CASE 
        WHEN t.balance = last_tx.balance_after THEN '✓ 一致'
        ELSE '✗ 不一致'
    END AS status
FROM teams t
LEFT JOIN (
    SELECT 
        team_id,
        balance_after,
        created_at
    FROM transactions
    WHERE (team_id, created_at) IN (
        SELECT team_id, MAX(created_at)
        FROM transactions
        GROUP BY team_id
    )
) last_tx ON t.id = last_tx.team_id
ORDER BY t.id;

-- 6. 显示每个团队前3条流水的余额变化
SELECT 
    '团队流水示例（前3条）' AS info,
    t.team_id,
    tm.team_name,
    t.id AS txn_id,
    t.txn_type,
    t.amount,
    t.balance_after,
    t.description,
    t.created_at
FROM transactions t
LEFT JOIN teams tm ON t.team_id = tm.id
WHERE t.id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY team_id ORDER BY created_at, id) AS rn
        FROM transactions
    ) ranked
    WHERE rn <= 3
)
ORDER BY t.team_id, t.created_at, t.id;

-- 清理临时表
DROP TEMPORARY TABLE IF EXISTS temp_transaction_balance;

SELECT '流水余额补全完成！' AS result;
