-- 修复 process 表的 template_id 外键约束
-- 将外键从引用 process_template(id) 改为引用 template_process(id)
-- 执行时间：2025-12-15

USE zzw_gx;

-- 1. 删除旧的外键约束
-- 根据错误信息，约束名称为 fk_process_template
-- 如果约束名称不同，请先执行：SHOW CREATE TABLE process; 查看实际约束名称
ALTER TABLE process DROP FOREIGN KEY fk_process_template;

-- 2. 清理现有的 template_id 数据
-- 由于旧的 template_id 引用 process_template 表，而新的表结构使用 template_process 表
-- 历史数据无法直接映射，所以将现有的 template_id 设置为 NULL
-- 注意：这不会影响历史数据的其他信息，只是清除了模板关联关系
UPDATE process SET template_id = NULL WHERE template_id IS NOT NULL;

-- 3. 创建新的外键约束，引用 template_process(id)
ALTER TABLE process 
ADD CONSTRAINT fk_process_template_process 
FOREIGN KEY (template_id) REFERENCES template_process(id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 说明：
-- - process.template_id 现在引用 template_process.id（模板-工序关联表的ID）
-- - 这与重构后的数据库结构一致，template_process 表存储模板和工序的关联关系
-- - 历史数据的 template_id 已被清空，新创建的工序会正确设置 template_id
-- - 如果第1步报错说约束不存在，可以跳过第1步直接执行第2步和第3步

