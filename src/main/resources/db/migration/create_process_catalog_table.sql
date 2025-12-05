-- 创建工序字典表（统一管理所有工序，支持顺序调整）
use zzw_gx;
CREATE TABLE IF NOT EXISTS process_catalog (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_name VARCHAR(100) NOT NULL UNIQUE COMMENT '工序名称（唯一）',
    process_code VARCHAR(50) UNIQUE COMMENT '工序编码（可选，用于程序识别）',
    description VARCHAR(500) COMMENT '工序描述',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序（用于调整工序顺序）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_process_name (process_name),
    INDEX idx_display_order (display_order),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序字典表（统一管理所有工序）';

-- 插入工序字典数据
INSERT INTO process_catalog (process_name, process_code, description, display_order, status, deleted) VALUES
('扒渣（平整场地）', 'PROCESS_001', '清理工作面，平整施工场地', 1, 1, 0),
('测量放样', 'PROCESS_002', '测量放样定位', 2, 1, 0),
('炮孔打设', 'PROCESS_003', '钻设炮孔', 3, 1, 0),
('装药爆破', 'PROCESS_004', '装药并进行爆破作业', 4, 1, 0),
('出渣排险', 'PROCESS_005', '清理爆破后的渣土和危石', 5, 1, 0),
('断面扫描（报检）', 'PROCESS_006', '进行断面扫描并报检', 6, 1, 0),
('初喷', 'PROCESS_007', '初次喷射混凝土', 7, 1, 0),
('拱架安装', 'PROCESS_008', '安装钢拱架', 8, 1, 0),
('锁脚打设', 'PROCESS_009', '打设锁脚锚杆', 9, 1, 0),
('锚杆打设', 'PROCESS_010', '打设系统锚杆', 10, 1, 0),
('超前打设', 'PROCESS_011', '打设超前支护', 11, 1, 0),
('报检', 'PROCESS_012', '质量检查报验', 12, 1, 0),
('喷射混凝土', 'PROCESS_013', '最终喷射混凝土', 13, 1, 0);

-- 修改process_template表，添加process_catalog_id字段
ALTER TABLE process_template 
ADD COLUMN process_catalog_id BIGINT COMMENT '工序字典ID（引用process_catalog表）' AFTER template_name,
ADD INDEX idx_process_catalog_id (process_catalog_id),
ADD UNIQUE KEY uk_template_process (template_name, process_catalog_id);


-- 添加外键约束
ALTER TABLE process_template
ADD CONSTRAINT fk_process_template_catalog 
FOREIGN KEY (process_catalog_id) REFERENCES process_catalog(id) 
ON DELETE RESTRICT ON UPDATE CASCADE;

-- 修改process表，添加process_catalog_id字段
ALTER TABLE process 
ADD COLUMN process_catalog_id BIGINT COMMENT '工序字典ID（引用process_catalog表，用于统一管理工序）' AFTER template_id,
ADD INDEX idx_process_catalog_id (process_catalog_id);

-- 迁移现有数据：根据process_name匹配process_catalog，设置process_catalog_id
-- 注意：如果process_name在process_catalog中不存在，process_catalog_id将保持为NULL
UPDATE process p
INNER JOIN process_catalog pc ON p.process_name = pc.process_name
SET p.process_catalog_id = pc.id
WHERE p.deleted = 0 AND p.process_catalog_id IS NULL;

-- 添加外键约束（只对非NULL的process_catalog_id生效）
ALTER TABLE process
ADD CONSTRAINT fk_process_catalog 
FOREIGN KEY (process_catalog_id) REFERENCES process_catalog(id) 
ON DELETE RESTRICT ON UPDATE CASCADE;

-- 可选：删除process_template表中的process_name字段（如果确定不再需要）
-- ALTER TABLE process_template DROP COLUMN process_name;

