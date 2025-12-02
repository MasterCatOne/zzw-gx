-- 创建数据库
CREATE DATABASE IF NOT EXISTS zzw_gx DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE zzw_gx;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '姓名',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN-管理员，WORKER-施工人员',
    id_card VARCHAR(18) COMMENT '身份证号（实名信息）',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '工点名称',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '项目状态：IN_PROGRESS-进行中，COMPLETED-已完成，PAUSED-已暂停',
    rock_level VARCHAR(20) COMMENT '围岩等级：LEVEL_I-I级，LEVEL_II-II级',
    current_cycle INT DEFAULT 0 COMMENT '当前循环次数',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 工序模板表
CREATE TABLE IF NOT EXISTS process_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    process_list TEXT COMMENT '工序列表（JSON格式）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序模板表';

-- 循环表
CREATE TABLE IF NOT EXISTS cycle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    cycle_number INT NOT NULL COMMENT '循环次数',
    control_duration INT COMMENT '控制时长标准（分钟）',
    start_date DATETIME COMMENT '开始日期',
    estimated_mileage DECIMAL(10, 2) COMMENT '预估里程（米）',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '循环状态：IN_PROGRESS-进行中，COMPLETED-已完成',
    advance_length DECIMAL(10, 2) DEFAULT 0 COMMENT '循环进尺（米）',
    template_id BIGINT COMMENT '工序模板ID',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_id (project_id),
    INDEX idx_cycle_number (cycle_number),
    FOREIGN KEY (project_id) REFERENCES project(id),
    FOREIGN KEY (template_id) REFERENCES process_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环表';

-- 工序表
CREATE TABLE IF NOT EXISTS process (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cycle_id BIGINT NOT NULL COMMENT '循环ID',
    name VARCHAR(100) NOT NULL COMMENT '工序名称',
    control_time INT NOT NULL COMMENT '控制时间（分钟）',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    status VARCHAR(20) DEFAULT 'NOT_STARTED' COMMENT '工序状态：NOT_STARTED-未开始，IN_PROGRESS-进行中，COMPLETED-已完成',
    operator_id BIGINT COMMENT '操作员ID',
    start_order INT COMMENT '开始顺序',
    advance_length DECIMAL(10, 2) DEFAULT 0 COMMENT '进尺长度（米）',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cycle_id (cycle_id),
    INDEX idx_status (status),
    INDEX idx_operator_id (operator_id),
    FOREIGN KEY (cycle_id) REFERENCES cycle(id),
    FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序表';

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    process_id BIGINT NOT NULL COMMENT '工序ID',
    worker_id BIGINT NOT NULL COMMENT '施工人员ID',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待完成，IN_PROGRESS-进行中，COMPLETED-已完成',
    estimated_start_time DATETIME COMMENT '预计开始时间',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_end_time DATETIME COMMENT '实际结束时间',
    overtime_reason TEXT COMMENT '超时原因',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_process_id (process_id),
    INDEX idx_worker_id (worker_id),
    INDEX idx_status (status),
    FOREIGN KEY (process_id) REFERENCES process(id),
    FOREIGN KEY (worker_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ============================================
-- 初始化数据
-- ============================================

-- 用户表数据
-- 密码都是 123456 (BCrypt加密)
INSERT INTO sys_user (username, password, real_name, role, id_card, phone, status, deleted) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', 'ADMIN', '110101199001011234', '13800138000', 1, 0),
('worker01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', 'WORKER', '110101199001011235', '13800138001', 1, 0),
('worker02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', 'WORKER', '110101199001011236', '13800138002', 1, 0),
('worker03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', 'WORKER', '110101199001011237', '13800138003', 1, 0),
('worker04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵六', 'WORKER', '110101199001011238', '13800138004', 1, 0);

-- 项目表数据
INSERT INTO project (name, status, rock_level, current_cycle, deleted) VALUES
('林坪隧道出口', 'IN_PROGRESS', 'LEVEL_I', 2, 0),
('林坪隧道入口', 'IN_PROGRESS', 'LEVEL_II', 1, 0),
('某某工点', 'IN_PROGRESS', 'LEVEL_I', 0, 0);

-- 工序模板表数据
INSERT INTO process_template (name, process_list, deleted) VALUES
('工序模板1', '[{"name":"扒渣","controlTime":120,"startOrder":1,"advanceLength":0.5},{"name":"炮孔打设","controlTime":120,"startOrder":2,"advanceLength":0.3},{"name":"装药爆破","controlTime":60,"startOrder":3,"advanceLength":0.2},{"name":"测量放样","controlTime":60,"startOrder":4,"advanceLength":0.1}]', 0),
('工序模板2', '[{"name":"测量放样","controlTime":60,"startOrder":1,"advanceLength":0.1},{"name":"扒渣","controlTime":120,"startOrder":2,"advanceLength":0.5},{"name":"炮孔打设","controlTime":120,"startOrder":3,"advanceLength":0.3},{"name":"装药爆破","controlTime":60,"startOrder":4,"advanceLength":0.2}]', 0);

-- 循环表数据（为项目1创建2个循环）
INSERT INTO cycle (project_id, cycle_number, control_duration, start_date, estimated_mileage, status, advance_length, template_id, deleted) VALUES
(1, 1, 360, '2025-11-01 08:00:00', 1.5, 'COMPLETED', 1.1, 1, 0),
(1, 2, 360, '2025-11-04 08:00:00', 1.5, 'IN_PROGRESS', 0.5, 1, 0),
(2, 1, 360, '2025-11-05 08:00:00', 1.2, 'IN_PROGRESS', 0.3, 2, 0);

-- 更新项目的当前循环次数
UPDATE project SET current_cycle = 2 WHERE id = 1;
UPDATE project SET current_cycle = 1 WHERE id = 2;

-- 工序表数据（为循环1创建已完成工序，为循环2创建进行中工序）
-- 循环1的工序（已完成）
INSERT INTO process (cycle_id, name, control_time, actual_start_time, actual_end_time, status, operator_id, start_order, advance_length, deleted) VALUES
(1, '扒渣', 120, '2025-11-01 08:00:00', '2025-11-01 09:40:00', 'COMPLETED', 2, 1, 0.5, 0),
(1, '炮孔打设', 120, '2025-11-01 09:40:00', '2025-11-01 11:50:00', 'COMPLETED', 3, 2, 0.3, 0),
(1, '装药爆破', 60, '2025-11-01 11:50:00', '2025-11-01 12:50:00', 'COMPLETED', 4, 3, 0.2, 0),
(1, '测量放样', 60, '2025-11-01 12:50:00', '2025-11-01 13:50:00', 'COMPLETED', 5, 4, 0.1, 0);

-- 循环2的工序（进行中）
INSERT INTO process (cycle_id, name, control_time, actual_start_time, actual_end_time, status, operator_id, start_order, advance_length, deleted) VALUES
(2, '扒渣', 120, '2025-11-04 08:00:00', '2025-11-04 09:40:00', 'COMPLETED', 2, 1, 0.5, 0),
(2, '炮孔打设', 120, '2025-11-04 09:40:00', '2025-11-04 11:50:00', 'COMPLETED', 3, 2, 0.3, 0),
(2, '装药爆破', 60, '2025-11-05 08:00:00', NULL, 'IN_PROGRESS', 4, 3, 0.2, 0),
(2, '测量放样', 60, NULL, NULL, 'NOT_STARTED', 5, 4, 0.1, 0);

-- 循环3的工序（进行中）
INSERT INTO process (cycle_id, name, control_time, actual_start_time, actual_end_time, status, operator_id, start_order, advance_length, deleted) VALUES
(3, '测量放样', 60, '2025-11-05 08:00:00', NULL, 'IN_PROGRESS', 2, 1, 0.1, 0),
(3, '扒渣', 120, NULL, NULL, 'NOT_STARTED', 3, 2, 0.5, 0),
(3, '炮孔打设', 120, NULL, NULL, 'NOT_STARTED', 4, 3, 0.3, 0),
(3, '装药爆破', 60, NULL, NULL, 'NOT_STARTED', 5, 4, 0.2, 0);

-- 任务表数据
-- 循环1的任务（已完成）
INSERT INTO task (process_id, worker_id, status, estimated_start_time, actual_start_time, actual_end_time, overtime_reason, deleted) VALUES
(1, 2, 'COMPLETED', '2025-11-01 08:00:00', '2025-11-01 08:00:00', '2025-11-01 09:40:00', NULL, 0),
(2, 3, 'COMPLETED', '2025-11-01 09:40:00', '2025-11-01 09:40:00', '2025-11-01 11:50:00', '设备故障导致超时', 0),
(3, 4, 'COMPLETED', '2025-11-01 11:50:00', '2025-11-01 11:50:00', '2025-11-01 12:50:00', NULL, 0),
(4, 5, 'COMPLETED', '2025-11-01 12:50:00', '2025-11-01 12:50:00', '2025-11-01 13:50:00', NULL, 0);

-- 循环2的任务（部分进行中）
INSERT INTO task (process_id, worker_id, status, estimated_start_time, actual_start_time, actual_end_time, overtime_reason, deleted) VALUES
(5, 2, 'COMPLETED', '2025-11-04 08:00:00', '2025-11-04 08:00:00', '2025-11-04 09:40:00', NULL, 0),
(6, 3, 'COMPLETED', '2025-11-04 09:40:00', '2025-11-04 09:40:00', '2025-11-04 11:50:00', NULL, 0),
(7, 4, 'IN_PROGRESS', '2025-11-05 08:00:00', '2025-11-05 08:00:00', NULL, NULL, 0),
(8, 5, 'PENDING', '2025-11-05 08:00:00', NULL, NULL, NULL, 0);

-- 循环3的任务（部分进行中）
INSERT INTO task (process_id, worker_id, status, estimated_start_time, actual_start_time, actual_end_time, overtime_reason, deleted) VALUES
(9, 2, 'IN_PROGRESS', '2025-11-05 08:00:00', '2025-11-05 08:00:00', NULL, NULL, 0),
(10, 3, 'PENDING', '2025-11-05 08:00:00', NULL, NULL, NULL, 0),
(11, 4, 'PENDING', '2025-11-05 08:00:00', NULL, NULL, NULL, 0),
(12, 5, 'PENDING', '2025-11-05 08:00:00', NULL, NULL, NULL, 0);

