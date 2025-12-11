-- 添加隧道工点和管理员对应关系
-- 密码都是 123456，BCrypt加密

-- 1. 创建管理员用户（如果不存在）
-- 谭涛 - 列屿隧道进口
INSERT IGNORE INTO sys_user (username, password, real_name, status, deleted) 
VALUES ('tantao', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '谭涛', 1, 0);
SET @tantao_user_id = (SELECT id FROM sys_user WHERE username = 'tantao' LIMIT 1);

-- 惠勇 - 列屿隧道出口
INSERT IGNORE INTO sys_user (username, password, real_name, status, deleted) 
VALUES ('huiyong', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '惠勇', 1, 0);
SET @huiyong_user_id = (SELECT id FROM sys_user WHERE username = 'huiyong' LIMIT 1);

-- 曹智明 - 林坪隧道进口、出口
INSERT IGNORE INTO sys_user (username, password, real_name, status, deleted) 
VALUES ('caozhiming', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '曹智明', 1, 0);
SET @caozhiming_user_id = (SELECT id FROM sys_user WHERE username = 'caozhiming' LIMIT 1);

-- 郑兰孔 - 青径村隧道进口
INSERT IGNORE INTO sys_user (username, password, real_name, status, deleted) 
VALUES ('zhenglankong', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑兰孔', 1, 0);
SET @zhenglankong_user_id = (SELECT id FROM sys_user WHERE username = 'zhenglankong' LIMIT 1);

-- 2. 为管理员用户分配ADMIN角色（如果还没有分配）
INSERT IGNORE INTO sys_user_role (user_id, role_id, deleted) 
SELECT @tantao_user_id, 2, 0 WHERE @tantao_user_id IS NOT NULL;

INSERT IGNORE INTO sys_user_role (user_id, role_id, deleted) 
SELECT @huiyong_user_id, 2, 0 WHERE @huiyong_user_id IS NOT NULL;

INSERT IGNORE INTO sys_user_role (user_id, role_id, deleted) 
SELECT @caozhiming_user_id, 2, 0 WHERE @caozhiming_user_id IS NOT NULL;

INSERT IGNORE INTO sys_user_role (user_id, role_id, deleted) 
SELECT @zhenglankong_user_id, 2, 0 WHERE @zhenglankong_user_id IS NOT NULL;

-- 3. 创建隧道节点（如果不存在）
-- 列屿隧道
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'TUNNEL', '列屿隧道', 'TUNNEL-LIEYU', '列屿隧道', 'IN_PROGRESS', 0);
SET @lieyu_tunnel_id = (SELECT id FROM project WHERE project_code = 'TUNNEL-LIEYU' LIMIT 1);

-- 林坪隧道
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'TUNNEL', '林坪隧道', 'TUNNEL-LINPING', '林坪隧道', 'IN_PROGRESS', 0);
SET @linping_tunnel_id = (SELECT id FROM project WHERE project_code = 'TUNNEL-LINPING' LIMIT 1);

-- 青径村隧道
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
VALUES (NULL, 'TUNNEL', '青径村隧道', 'TUNNEL-QINGJING', '青径村隧道', 'IN_PROGRESS', 0);
SET @qingjing_tunnel_id = (SELECT id FROM project WHERE project_code = 'TUNNEL-QINGJING' LIMIT 1);

-- 4. 创建工点（如果不存在）
-- 列屿隧道进口
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
SELECT @lieyu_tunnel_id, 'SITE', '列屿隧道进口', 'SITE-LIEYU-ENTRANCE', '列屿隧道进口工点', 'IN_PROGRESS', 0
WHERE @lieyu_tunnel_id IS NOT NULL;
SET @lieyu_entrance_id = (SELECT id FROM project WHERE project_code = 'SITE-LIEYU-ENTRANCE' LIMIT 1);

-- 列屿隧道出口
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
SELECT @lieyu_tunnel_id, 'SITE', '列屿隧道出口', 'SITE-LIEYU-EXIT', '列屿隧道出口工点', 'IN_PROGRESS', 0
WHERE @lieyu_tunnel_id IS NOT NULL;
SET @lieyu_exit_id = (SELECT id FROM project WHERE project_code = 'SITE-LIEYU-EXIT' LIMIT 1);

-- 林坪隧道进口
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
SELECT @linping_tunnel_id, 'SITE', '林坪隧道进口', 'SITE-LINPING-ENTRANCE', '林坪隧道进口工点', 'IN_PROGRESS', 0
WHERE @linping_tunnel_id IS NOT NULL;
SET @linping_entrance_id = (SELECT id FROM project WHERE project_code = 'SITE-LINPING-ENTRANCE' LIMIT 1);

-- 林坪隧道出口
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
SELECT @linping_tunnel_id, 'SITE', '林坪隧道出口', 'SITE-LINPING-EXIT', '林坪隧道出口工点', 'IN_PROGRESS', 0
WHERE @linping_tunnel_id IS NOT NULL;
SET @linping_exit_id = (SELECT id FROM project WHERE project_code = 'SITE-LINPING-EXIT' LIMIT 1);

-- 青径村隧道进口
INSERT IGNORE INTO project (parent_id, node_type, project_name, project_code, project_description, project_status, deleted)
SELECT @qingjing_tunnel_id, 'SITE', '青径村隧道进口', 'SITE-QINGJING-ENTRANCE', '青径村隧道进口工点', 'IN_PROGRESS', 0
WHERE @qingjing_tunnel_id IS NOT NULL;
SET @qingjing_entrance_id = (SELECT id FROM project WHERE project_code = 'SITE-QINGJING-ENTRANCE' LIMIT 1);

-- 5. 分配管理员到工点
-- 列屿隧道进口：谭涛
INSERT IGNORE INTO sys_user_project (user_id, project_id, deleted) 
SELECT @tantao_user_id, @lieyu_entrance_id, 0 
WHERE @tantao_user_id IS NOT NULL AND @lieyu_entrance_id IS NOT NULL;

-- 列屿隧道出口：惠勇
INSERT IGNORE INTO sys_user_project (user_id, project_id, deleted) 
SELECT @huiyong_user_id, @lieyu_exit_id, 0 
WHERE @huiyong_user_id IS NOT NULL AND @lieyu_exit_id IS NOT NULL;

-- 林坪隧道进口：曹智明
INSERT IGNORE INTO sys_user_project (user_id, project_id, deleted) 
SELECT @caozhiming_user_id, @linping_entrance_id, 0 
WHERE @caozhiming_user_id IS NOT NULL AND @linping_entrance_id IS NOT NULL;

-- 林坪隧道出口：曹智明
INSERT IGNORE INTO sys_user_project (user_id, project_id, deleted) 
SELECT @caozhiming_user_id, @linping_exit_id, 0 
WHERE @caozhiming_user_id IS NOT NULL AND @linping_exit_id IS NOT NULL;

-- 青径村隧道进口：郑兰孔
INSERT IGNORE INTO sys_user_project (user_id, project_id, deleted) 
SELECT @zhenglankong_user_id, @qingjing_entrance_id, 0 
WHERE @zhenglankong_user_id IS NOT NULL AND @qingjing_entrance_id IS NOT NULL;

