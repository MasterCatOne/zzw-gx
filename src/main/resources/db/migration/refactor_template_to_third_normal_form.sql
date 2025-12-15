-- 重构模板表结构以满足第三范式
-- 实现：工点和模板多对多关系，模板和工序一对多关系
-- 执行时间：2025-12-12

USE zzw_gx;

-- ============================================
-- 第一步：创建新的表结构
-- ============================================

-- 1. 创建模板表（模板基本信息，满足第三范式）
CREATE TABLE IF NOT EXISTS template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL UNIQUE COMMENT '模板名称（唯一）',
    template_description VARCHAR(500) COMMENT '模板描述',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_name (template_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表（模板基本信息）';

-- 2. 创建工点-模板关联表（实现多对多关系）
CREATE TABLE IF NOT EXISTS project_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id BIGINT NOT NULL COMMENT '工点ID（引用project表，node_type必须为SITE）',
    template_id BIGINT NOT NULL COMMENT '模板ID（引用template表）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_template (project_id, template_id),
    INDEX idx_project_id (project_id),
    INDEX idx_template_id (template_id),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (template_id) REFERENCES template(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工点-模板关联表（多对多关系）';

-- 3. 创建模板-工序关联表（模板和工序的一对多关系）
-- 注意：一个模板中同一个工序可以出现多次（如"测量放样"可以出现两次），
-- 所以唯一约束应该是 (template_id, default_order)，而不是 (template_id, process_catalog_id)
CREATE TABLE IF NOT EXISTS template_process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID（引用template表）',
    process_catalog_id BIGINT NOT NULL COMMENT '工序字典ID（引用process_catalog表）',
    control_time INT NOT NULL COMMENT '控制时间（分钟）',
    default_order INT NOT NULL COMMENT '默认顺序（在该模板中的顺序，唯一）',
    description VARCHAR(500) COMMENT '工序描述（可选，如果为空则使用工序字典中的描述）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_id (template_id),
    INDEX idx_process_catalog_id (process_catalog_id),
    INDEX idx_default_order (default_order),
    UNIQUE KEY uk_template_order (template_id, default_order),
    FOREIGN KEY (template_id) REFERENCES template(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (process_catalog_id) REFERENCES process_catalog(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板-工序关联表（模板和工序的一对多关系）';

-- ============================================
-- 第二步：初始化数据示例（可选）
-- ============================================

-- 以下为示例数据，可根据实际需求修改或删除

-- 1. 创建模板示例
-- INSERT INTO template (template_name, template_description, deleted) VALUES
-- ('标准模板', '标准施工模板', 0),
-- ('快速模板', '快速施工模板', 0);

-- 2. 创建工点-模板关联示例（假设工点ID为7和8）
-- INSERT INTO project_template (project_id, template_id, deleted) VALUES
-- (7, 1, 0),  -- 工点7使用标准模板
-- (8, 1, 0),  -- 工点8也使用标准模板
-- (8, 2, 0);  -- 工点8也可以使用快速模板

-- 3. 创建模板-工序关联示例（假设标准模板ID为1，工序字典ID从1开始）
-- INSERT INTO template_process (template_id, process_catalog_id, control_time, default_order, description, deleted) VALUES
-- (1, 1, 120, 1, NULL, 0),  -- 标准模板：扒渣（平整场地），120分钟，顺序1
-- (1, 2, 60, 2, NULL, 0),   -- 标准模板：测量放样，60分钟，顺序2
-- (1, 3, 120, 3, NULL, 0),  -- 标准模板：炮孔打设，120分钟，顺序3
-- (1, 4, 60, 4, NULL, 0);    -- 标准模板：装药爆破，60分钟，顺序4
-- ... 更多工序

-- ============================================
-- 说明：
-- 1. 新的表结构满足第三范式：
--    - template 表只存储模板基本信息，无冗余
--    - project_template 表实现工点和模板的多对多关系
--    - template_process 表实现模板和工序的一对多关系
-- 
-- 2. 表关系说明：
--    - 工点（project）和模板（template）是多对多关系，通过 project_template 表关联
--    - 模板（template）和工序（process_catalog）是一对多关系，通过 template_process 表关联
--    - 一个模板可以包含多个工序，一个工序可以在多个模板中出现（甚至同一模板中出现多次）
-- 
-- 3. 使用说明：
--    - 创建模板：在 template 表中插入模板基本信息
--    - 关联工点：在 project_template 表中插入工点和模板的关联关系
--    - 配置工序：在 template_process 表中插入模板包含的工序及其控制时间
-- 
-- 4. 注意事项：
--    - template_process 表的唯一约束是 (template_id, default_order)，允许同一工序在模板中出现多次
--    - 如果需要在 process 表中记录使用的模板工序，可以引用 template_process.id
-- ============================================

