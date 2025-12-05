-- 为process表添加overtime_reason字段，用于存储工序超时原因
-- 执行时间：2025-12-05
USE zzw_gx;

-- 添加overtime_reason字段（允许为空，因为历史数据可能没有超时原因）
ALTER TABLE process 
ADD COLUMN overtime_reason VARCHAR(500) COMMENT '超时原因' AFTER template_id;

-- 添加索引（可选，如果需要根据超时原因查询）
-- ALTER TABLE process 
-- ADD INDEX idx_overtime_reason (overtime_reason(255));

