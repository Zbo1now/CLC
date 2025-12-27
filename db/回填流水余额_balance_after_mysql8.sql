-- 回填 transactions.balance_after（MySQL 8+ 推荐）
-- 适用场景：balance_after 历史为空；teams.balance 为当前余额；transactions.amount 为每笔余额增量（正数入账/负数扣减）
-- 计算思路：对每个 team，从“最新流水”往前回推：
--   balance_after(某笔) = 当前余额 - SUM(该笔之后的所有amount)
USE campuscoin;

-- 注意：执行前请确保已执行 db/transaction_enhancement.sql，使 transactions 表存在 balance_after 字段。
-- 本脚本不会修改 teams.balance，只会补齐 transactions.balance_after。

WITH ordered AS (
    SELECT
        t.id,
        t.team_id,
        tm.balance AS current_balance,
        COALESCE(
            SUM(t.amount) OVER (
                PARTITION BY t.team_id
                ORDER BY t.created_at DESC, t.id DESC
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS newer_sum
    FROM transactions t
    JOIN teams tm ON tm.id = t.team_id
)
UPDATE transactions t
JOIN ordered o ON o.id = t.id
SET t.balance_after = o.current_balance - o.newer_sum
WHERE t.balance_after IS NULL;

-- 验证
SELECT
    COUNT(*) AS total,
    SUM(CASE WHEN balance_after IS NULL THEN 1 ELSE 0 END) AS null_cnt,
    SUM(CASE WHEN balance_after IS NOT NULL THEN 1 ELSE 0 END) AS filled_cnt
FROM transactions;


