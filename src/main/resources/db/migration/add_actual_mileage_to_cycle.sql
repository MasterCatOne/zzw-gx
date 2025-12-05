-- 为cycle表添加actual_mileage字段，用于记录循环实际结束里程
-- 执行时间：2025-12-05
USE zzw_gx;

-- 添加actual_mileage字段（实际里程，初喷后的测量放样结束后填报）
ALTER TABLE cycle 
ADD COLUMN actual_mileage DECIMAL(10, 2) COMMENT '实际里程（米），初喷后的测量放样结束后填报' AFTER estimated_mileage;

