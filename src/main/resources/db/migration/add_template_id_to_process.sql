-- 为process表添加template_id字段，建立工序与工序模板的关联关系
-- 执行时间：2025-11-05
use zzw_gx;
-- 1. 添加template_id字段（允许为空，因为历史数据可能没有模板ID）
ALTER TABLE process 
ADD COLUMN template_id BIGINT COMMENT '工序模板ID（记录工序来源模板）' AFTER advance_length;

-- 2. 添加索引
ALTER TABLE process 
ADD INDEX idx_template_id (template_id);

-- 3. 添加外键约束（关联到process_template表）
ALTER TABLE process 
ADD CONSTRAINT fk_process_template 
FOREIGN KEY (template_id) REFERENCES process_template(id) 
ON DELETE RESTRICT ON UPDATE CASCADE;

