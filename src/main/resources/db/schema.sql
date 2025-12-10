-- 创建数据库
CREATE DATABASE IF NOT EXISTS zzw_gx DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zzw_gx;

-- ============================================
-- 用户权限体系表
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '姓名',
    id_card VARCHAR(18) COMMENT '身份证号（实名信息）',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码：SYSTEM_ADMIN-系统管理员，ADMIN-管理员，WORKER-施工人员',
    role_description VARCHAR(200) COMMENT '角色描述',
    role_status TINYINT DEFAULT 1 COMMENT '角色状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role_code (role_code),
    INDEX idx_role_status (role_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
   user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================
-- 业务表（树状结构）
-- ============================================

-- 项目树表（顶层项目/标段/隧道/工点等统一管理）
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父节点ID，为空表示顶级项目',
    node_type VARCHAR(30) NOT NULL COMMENT '节点类型：PROJECT-项目，SECTION-标段，TUNNEL-隧道，SITE-工点等',
    project_name VARCHAR(100) NOT NULL COMMENT '节点名称',
    project_code VARCHAR(50) UNIQUE COMMENT '节点编号，如：LINE1-SEC1、TUNNEL-001',
    project_description TEXT COMMENT '节点描述',
    project_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '节点状态：IN_PROGRESS-进行中，COMPLETED-已完成，PAUSED-已暂停',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id),
    INDEX idx_node_type (node_type),
    INDEX idx_project_name (project_name),
    INDEX idx_project_code (project_code),
    INDEX idx_project_status (project_status),
    CONSTRAINT fk_project_parent FOREIGN KEY (parent_id) REFERENCES project(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目树表（项目/标段/隧道/工点等统一管理）';

-- 用户工点关联表（管理员管理的工点/节点）
CREATE TABLE IF NOT EXISTS sys_user_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID（管理员）',
    project_id BIGINT NOT NULL COMMENT '被管理的节点ID，通常为工点',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_project (user_id, project_id),
    INDEX idx_user_id (user_id),
    INDEX idx_project_id (project_id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户工点关联表（管理员管理的工点）';

-- 循环表
CREATE TABLE IF NOT EXISTS cycle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id BIGINT NOT NULL COMMENT '所属工点ID',
    cycle_number INT NOT NULL COMMENT '循环次数',
    control_duration INT COMMENT '控制时长标准（分钟）',
    start_date DATETIME COMMENT '开始日期',
    end_date DATETIME COMMENT '结束日期',
    estimated_start_date DATETIME COMMENT '预计开始日期',
    estimated_end_date DATETIME COMMENT '预计结束日期',
    estimated_mileage DECIMAL(10, 2) COMMENT '预估里程（米）',
    actual_mileage DECIMAL(10, 2) COMMENT '实际里程（米）',
    development_method VARCHAR(50) COMMENT '开挖/开发方式，如：台阶法',
    cycle_status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '循环状态：IN_PROGRESS-进行中，COMPLETED-已完成',
    advance_length DECIMAL(10, 2) DEFAULT 0 COMMENT '循环进尺（米）',
    rock_level VARCHAR(20) COMMENT '围岩等级：LEVEL_I-I级，LEVEL_II-II级，LEVEL_III-III级，LEVEL_IV-IV级，LEVEL_V-V级，LEVEL_VI-VI级',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_id (project_id),
    INDEX idx_cycle_number (cycle_number),
    INDEX idx_cycle_status (cycle_status),
    INDEX idx_rock_level (rock_level),
    UNIQUE KEY uk_project_cycle_number (project_id, cycle_number),
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环表';

-- 工序表
CREATE TABLE IF NOT EXISTS process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cycle_id BIGINT NOT NULL COMMENT '循环ID',
    process_name VARCHAR(100) NOT NULL COMMENT '工序名称',
    control_time INT NOT NULL COMMENT '控制时间（分钟）',
    estimated_start_time DATETIME COMMENT '预计开始时间',
    estimated_end_time DATETIME COMMENT '预计结束时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    process_status VARCHAR(20) DEFAULT 'NOT_STARTED' COMMENT '工序状态：NOT_STARTED-未开始，IN_PROGRESS-进行中，COMPLETED-已完成',
    operator_id BIGINT COMMENT '操作员ID',
    start_order INT COMMENT '开始顺序',
    advance_length DECIMAL(10, 2) DEFAULT 0 COMMENT '进尺长度（米）',
    template_id BIGINT COMMENT '工序模板ID（记录工序来源模板）',
    process_catalog_id BIGINT COMMENT '工序字典ID（引用process_catalog表，用于统一管理工序）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cycle_id (cycle_id),
    INDEX idx_process_status (process_status),
    INDEX idx_operator_id (operator_id),
    INDEX idx_start_order (start_order),
    INDEX idx_template_id (template_id),
    INDEX idx_process_catalog_id (process_catalog_id),
    FOREIGN KEY (cycle_id) REFERENCES cycle(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (template_id) REFERENCES process_template(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (process_catalog_id) REFERENCES process_catalog(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序表';

-- 工序操作日志表（记录施工人员对工序的关键操作：开始、完成、填报原因等）
CREATE TABLE IF NOT EXISTS process_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_id BIGINT NOT NULL COMMENT '工序ID',
    user_id BIGINT COMMENT '操作人ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型：START/COMPLETED/COMPLETED_AND_NEXT/OVERTIME_REASON/CREATE_AND_START 等',
    remark VARCHAR(500) COMMENT '备注信息，如超时原因等',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_process_id (process_id),
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    FOREIGN KEY (process_id) REFERENCES process(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序操作日志表';

-- 工序字典表（统一管理所有工序，支持顺序调整）
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

-- 工序模板表（模板与工序的关联表，设置每个模板中工序的控制时间）
CREATE TABLE IF NOT EXISTS process_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    process_catalog_id BIGINT NOT NULL COMMENT '工序字典ID（引用process_catalog表）',
    control_time INT NOT NULL COMMENT '控制时间（分钟）',
    default_order INT NOT NULL COMMENT '默认顺序（在该模板中的顺序）',
    description VARCHAR(500) COMMENT '工序描述（可选，如果为空则使用工序字典中的描述）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_name (template_name),
    INDEX idx_process_catalog_id (process_catalog_id),
    INDEX idx_default_order (default_order),
    UNIQUE KEY uk_template_process (template_name, process_catalog_id),
    FOREIGN KEY (process_catalog_id) REFERENCES process_catalog(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序模板表（模板与工序的关联表）';

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_id BIGINT NOT NULL COMMENT '工序ID',
    worker_id BIGINT NOT NULL COMMENT '施工人员ID',
    task_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待完成，IN_PROGRESS-进行中，COMPLETED-已完成',
    estimated_start_time DATETIME COMMENT '预计开始时间',
    estimated_end_time DATETIME COMMENT '预计结束时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    overtime_reason TEXT COMMENT '超时原因',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_process_id (process_id),
    INDEX idx_worker_id (worker_id),
    INDEX idx_task_status (task_status),
    FOREIGN KEY (process_id) REFERENCES process(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (worker_id) REFERENCES sys_user(id) ON DELETE RESTRICT ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ============================================
-- 初始化数据
-- ============================================

-- 角色表数据
INSERT INTO sys_role (name, role_code, role_description, role_status, deleted) VALUES
                                                                                   ('系统管理员', 'SYSTEM_ADMIN', '系统管理员，拥有所有权限', 1, 0),
                                                                                   ('管理员', 'ADMIN', '管理员，负责项目管理', 1, 0),
                                                                                   ('施工人员', 'WORKER', '施工人员，负责具体施工任务', 1, 0);

-- 用户表数据（密码都是 123456，BCrypt加密）
INSERT INTO sys_user (username, password, real_name, id_card, phone, status, deleted) VALUES
                                                                                          ('system_admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', '110101199001011230', '13800138000', 1, 0),
                                                                                          ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', '110101199001011234', '13800138001', 1, 0),
                                                                                          ('worker01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', '110101199001011235', '13800138002', 1, 0),
                                                                                          ('worker02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', '110101199001011236', '13800138003', 1, 0),
                                                                                          ('worker03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', '110101199001011237', '13800138004', 1, 0),
                                                                                          ('worker04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵六', '110101199001011238', '13800138005', 1, 0);

-- 用户角色关联表数据
INSERT INTO sys_user_role (user_id, role_id, deleted) VALUES
                                                          (1, 1, 0), -- system_admin -> SYSTEM_ADMIN
                                                          (2, 2, 0), -- admin -> ADMIN
                                                          (3, 3, 0), -- worker01 -> WORKER
                                                          (4, 3, 0), -- worker02 -> WORKER
                                                          (5, 3, 0), -- worker03 -> WORKER
                                                          (6, 3, 0); -- worker04 -> WORKER

-- 示例业务数据
-- 项目树数据（项目 -> 标段 -> 隧道 -> 工点）
-- 地铁1号线
INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'PROJECT', '地铁1号线', 'LINE1', '地铁1号线建设工程', 'IN_PROGRESS', 0);
SET @line1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line1, 'SECTION', '1号线第1标段', 'LINE1-SEC1', '地铁1号线第1标段，从A站到B站', 'IN_PROGRESS', 0);
SET @line1_sec1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line1, 'SECTION', '1号线第2标段', 'LINE1-SEC2', '地铁1号线第2标段，从B站到C站', 'IN_PROGRESS', 0);
SET @line1_sec2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line1_sec1, 'TUNNEL', '1标段上行隧道', 'TUNNEL-001', '1号线第1标段上行隧道', 'IN_PROGRESS', 0);
SET @line1_sec1_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line1_sec1, 'TUNNEL', '1标段下行隧道', 'TUNNEL-002', '1号线第1标段下行隧道', 'IN_PROGRESS', 0);
SET @line1_sec1_tunnel2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line1_sec2, 'TUNNEL', '2标段上行隧道', 'TUNNEL-003', '1号线第2标段上行隧道', 'IN_PROGRESS', 0);
SET @line1_sec2_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted) VALUES
(@line1_sec1_tunnel1, 'SITE', '上行隧道入口工点', 'SITE-001', '位于1标段上行隧道入口', 'IN_PROGRESS', 0),
(@line1_sec1_tunnel1, 'SITE', '上行隧道中间工点', 'SITE-002', '位于1标段上行隧道中部', 'IN_PROGRESS', 0),
(@line1_sec1_tunnel1, 'SITE', '上行隧道出口工点', 'SITE-003', '位于1标段上行隧道出口', 'IN_PROGRESS', 0);

-- 地铁2号线
INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'PROJECT', '地铁2号线', 'LINE2', '地铁2号线建设工程', 'IN_PROGRESS', 0);
SET @line2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line2, 'SECTION', '2号线第1标段', 'LINE2-SEC1', '地铁2号线第1标段，从D站到E站', 'IN_PROGRESS', 0);
SET @line2_sec1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line2, 'SECTION', '2号线第2标段', 'LINE2-SEC2', '地铁2号线第2标段，从E站到F站', 'IN_PROGRESS', 0);
SET @line2_sec2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line2_sec1, 'TUNNEL', '2号线1标段上行隧道', 'TUNNEL-101', '2号线第1标段上行隧道', 'IN_PROGRESS', 0);
SET @line2_sec1_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line2_sec1, 'TUNNEL', '2号线1标段下行隧道', 'TUNNEL-102', '2号线第1标段下行隧道', 'IN_PROGRESS', 0);
SET @line2_sec1_tunnel2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line2_sec2, 'TUNNEL', '2号线2标段上行隧道', 'TUNNEL-103', '2号线第2标段上行隧道', 'IN_PROGRESS', 0);
SET @line2_sec2_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted) VALUES
(@line2_sec1_tunnel1, 'SITE', '2号线1标段上行入口', 'SITE-101', '2号线1标段上行隧道入口工点', 'IN_PROGRESS', 0),
(@line2_sec1_tunnel1, 'SITE', '2号线1标段上行出口', 'SITE-102', '2号线1标段上行隧道出口工点', 'IN_PROGRESS', 0),
(@line2_sec1_tunnel2, 'SITE', '2号线1标段下行中段', 'SITE-103', '2号线1标段下行隧道中段工点', 'IN_PROGRESS', 0),
(@line2_sec2_tunnel1, 'SITE', '2号线2标段上行中段', 'SITE-104', '2号线2标段上行隧道中段工点', 'IN_PROGRESS', 0),
(@line2_sec2_tunnel1, 'SITE', '2号线2标段上行出口', 'SITE-105', '2号线2标段上行隧道出口工点', 'IN_PROGRESS', 0);

-- 地铁3号线
INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'PROJECT', '地铁3号线', 'LINE3', '地铁3号线建设工程', 'IN_PROGRESS', 0);
SET @line3 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line3, 'SECTION', '3号线海滨段', 'LINE3-SEC1', '地铁3号线海滨段，从G站到H站', 'IN_PROGRESS', 0);
SET @line3_sec1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line3, 'SECTION', '3号线中心城段', 'LINE3-SEC2', '地铁3号线中心城段，从H站到I站', 'IN_PROGRESS', 0);
SET @line3_sec2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line3_sec1, 'TUNNEL', '3号线海滨段隧道', 'TUNNEL-201', '3号线海滨段主隧道', 'IN_PROGRESS', 0);
SET @line3_sec1_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line3_sec2, 'TUNNEL', '3号线上行隧道', 'TUNNEL-202', '3号线中心城段上行隧道', 'IN_PROGRESS', 0);
SET @line3_sec2_tunnel1 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (@line3_sec2, 'TUNNEL', '3号线下行隧道', 'TUNNEL-203', '3号线中心城段下行隧道', 'IN_PROGRESS', 0);
SET @line3_sec2_tunnel2 := LAST_INSERT_ID();

INSERT INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted) VALUES
(@line3_sec1_tunnel1, 'SITE', '海滨段北口工点', 'SITE-201', '3号线海滨段北口工点', 'IN_PROGRESS', 0),
(@line3_sec1_tunnel1, 'SITE', '海滨段南口工点', 'SITE-202', '3号线海滨段南口工点', 'IN_PROGRESS', 0),
(@line3_sec2_tunnel1, 'SITE', '中心城上行中段工点', 'SITE-203', '3号线中心城上行中段', 'IN_PROGRESS', 0),
(@line3_sec2_tunnel2, 'SITE', '中心城下行中段工点', 'SITE-204', '3号线中心城下行中段', 'IN_PROGRESS', 0);

-- 循环数据
INSERT INTO cycle (project_id, cycle_number, control_duration, start_date, estimated_mileage, cycle_status, advance_length, rock_level, deleted) VALUES
                                                                                                                                                   (7, 1, 360, '2025-11-01 08:00:00', 1.5, 'COMPLETED', 1.1, 'LEVEL_I', 0),
                                                                                                                                                   (7, 2, 360, '2025-11-04 08:00:00', 1.5, 'IN_PROGRESS', 0.5, 'LEVEL_II', 0),
                                                                                                                                                   (8, 1, 360, '2025-11-05 08:00:00', 1.2, 'IN_PROGRESS', 0.3, 'LEVEL_I', 0);

-- 工序数据
INSERT INTO process (cycle_id, process_name, control_time, actual_start_time, actual_end_time, process_status, operator_id, start_order, advance_length, deleted) VALUES
-- 循环1的工序（已完成）
(1, '扒渣', 120, '2025-11-01 08:00:00', '2025-11-01 09:40:00', 'COMPLETED', 3, 1, 0.5, 0),
(1, '炮孔打设', 120, '2025-11-01 09:40:00', '2025-11-01 11:50:00', 'COMPLETED', 4, 2, 0.3, 0),
(1, '装药爆破', 60, '2025-11-01 11:50:00', '2025-11-01 12:50:00', 'COMPLETED', 5, 3, 0.2, 0),
(1, '测量放样', 60, '2025-11-01 12:50:00', '2025-11-01 13:50:00', 'COMPLETED', 6, 4, 0.1, 0),
-- 循环2的工序（进行中）
(2, '扒渣', 120, '2025-11-04 08:00:00', '2025-11-04 09:40:00', 'COMPLETED', 3, 1, 0.5, 0),
(2, '炮孔打设', 120, '2025-11-04 09:40:00', '2025-11-04 11:50:00', 'COMPLETED', 4, 2, 0.3, 0),
(2, '装药爆破', 60, '2025-11-05 08:00:00', NULL, 'IN_PROGRESS', 5, 3, 0.2, 0),
(2, '测量放样', 60, NULL, NULL, 'NOT_STARTED', 6, 4, 0.1, 0);

-- 任务数据
INSERT INTO task (process_id, worker_id, task_status, actual_start_time, actual_end_time, deleted) VALUES
                                                                                                       (1, 3, 'COMPLETED', '2025-11-01 08:00:00', '2025-11-01 09:40:00', 0),
                                                                                                       (2, 4, 'COMPLETED', '2025-11-01 09:40:00', '2025-11-01 11:50:00', 0),
                                                                                                       (3, 5, 'COMPLETED', '2025-11-01 11:50:00', '2025-11-01 12:50:00', 0),
                                                                                                       (4, 6, 'COMPLETED', '2025-11-01 12:50:00', '2025-11-01 13:50:00', 0),
                                                                                                       (5, 3, 'COMPLETED', '2025-11-04 08:00:00', '2025-11-04 09:40:00', 0),
                                                                                                       (6, 4, 'COMPLETED', '2025-11-04 09:40:00', '2025-11-04 11:50:00', 0),
                                                                                                       (7, 5, 'IN_PROGRESS', '2025-11-05 08:00:00', NULL, 0);

-- 用户工点关联表数据（为管理员分配管理的工点）
-- 系统管理员（user_id=1）不需要关联记录，可以查看所有工点
-- 普通管理员（user_id=2）管理工点1和工点2
INSERT INTO sys_user_project (user_id, project_id, deleted) VALUES
                                                               (2, 7, 0),  -- admin 管理工点1（上行隧道入口工点）
                                                               (2, 8, 0);  -- admin 管理工点2（上行隧道中间工点）

-- 工序字典数据（统一管理所有工序）
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

-- 工序模板数据（模板与工序的关联，设置每个模板中工序的控制时间）
-- 标准模板（使用process_catalog_id引用工序字典）
INSERT INTO process_template (template_name, process_catalog_id, control_time, default_order, description, deleted) VALUES
('标准模板', 1, 120, 1, NULL, 0),  -- 扒渣（平整场地）
('标准模板', 2, 60, 2, NULL, 0),   -- 测量放样
('标准模板', 3, 120, 3, NULL, 0),  -- 炮孔打设
('标准模板', 4, 60, 4, NULL, 0),   -- 装药爆破
('标准模板', 5, 90, 5, NULL, 0),   -- 出渣排险
('标准模板', 6, 30, 6, NULL, 0),   -- 断面扫描（报检）
('标准模板', 7, 60, 7, NULL, 0),   -- 初喷
('标准模板', 2, 60, 8, '二次测量放样', 0),  -- 测量放样（第二次，同一个工序字典ID）
('标准模板', 8, 120, 9, NULL, 0),  -- 拱架安装
('标准模板', 9, 90, 10, NULL, 0),  -- 锁脚打设
('标准模板', 10, 90, 11, NULL, 0), -- 锚杆打设
('标准模板', 11, 120, 12, NULL, 0), -- 超前打设
('标准模板', 12, 30, 13, NULL, 0), -- 报检
('标准模板', 13, 120, 14, NULL, 0); -- 喷射混凝土

-- 模板2
INSERT INTO process_template (template_name, process_catalog_id, control_time, default_order, description, deleted) VALUES
('模板2', 1, 100, 1, NULL, 0),  -- 扒渣（平整场地）
('模板2', 2, 50, 2, NULL, 0),   -- 测量放样
('模板2', 3, 100, 3, NULL, 0),  -- 炮孔打设
('模板2', 4, 50, 4, NULL, 0),   -- 装药爆破
('模板2', 5, 80, 5, NULL, 0),   -- 出渣排险
('模板2', 6, 30, 6, NULL, 0),   -- 断面扫描（报检）
('模板2', 7, 50, 7, NULL, 0),   -- 初喷
('模板2', 2, 50, 8, '二次测量放样', 0),  -- 测量放样（第二次）
('模板2', 8, 100, 9, NULL, 0),  -- 拱架安装
('模板2', 9, 80, 10, NULL, 0),  -- 锁脚打设
('模板2', 10, 80, 11, NULL, 0), -- 锚杆打设
('模板2', 11, 100, 12, NULL, 0), -- 超前打设
('模板2', 12, 30, 13, NULL, 0), -- 报检
('模板2', 13, 100, 14, NULL, 0); -- 喷射混凝土

