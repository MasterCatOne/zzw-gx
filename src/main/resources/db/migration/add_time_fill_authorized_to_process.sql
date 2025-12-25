-- 为process表添加时间补填授权标志字段
-- 执行时间：2025-12-22
USE zzw_gx;

-- 添加time_fill_authorized字段（允许为空，因为历史数据可能没有授权标志）
ALTER TABLE process 
ADD COLUMN time_fill_authorized TINYINT DEFAULT 0 COMMENT '时间补填授权标志：0-未授权，1-已授权' AFTER overtime_reason;

