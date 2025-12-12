-- 添加工点ID字段到工序模板表
-- 实现工点和模板的一对多关系

-- 添加 site_id 字段
ALTER TABLE process_template 
ADD COLUMN site_id BIGINT COMMENT '工点ID（引用project表，node_type必须为SITE）' AFTER template_name;

-- 添加外键约束
ALTER TABLE process_template 
ADD CONSTRAINT fk_process_template_site 
FOREIGN KEY (site_id) REFERENCES project(id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- 添加索引
ALTER TABLE process_template 
ADD INDEX idx_site_id (site_id);

-- 删除旧的唯一约束
ALTER TABLE process_template 
DROP INDEX uk_template_process;

-- 添加新的唯一约束（同一工点下，模板名称+工序字典ID唯一）
ALTER TABLE process_template 
ADD UNIQUE KEY uk_site_template_process (site_id, template_name, process_catalog_id);

