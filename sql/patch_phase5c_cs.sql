-- ==============================================================
-- 翔云·低空智慧物流运营平台 阶段5C 客服角色与工单分派权限增量补丁
-- 功能说明：
-- 1. 扩充角色字典：增加 ROLE_CS (商户客服专员)
-- 2. 授权 ROLE_CS 工单查看/答复/办结权限，授权 ROLE_MERCHANT 工单分派权限
-- 3. 新增顺丰商户客服专员预置账号 cs_sf / 123456
-- ==============================================================

-- 1. 扩充系统角色字典
INSERT INTO uam_role (role_id, role_code, role_name, description)
VALUES (8, 'ROLE_CS', '商户客服专员', '负责客户服务工单的受理、协同跟进、答复与办结关闭')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- 2. 授权商户客服专员权限
DELETE FROM uam_role_grant WHERE role_id = 8;
INSERT INTO uam_role_grant (role_id, perm_code, perm_name) VALUES
(8, 'ticket:view', '客户服务工单列表与详情查看'),
(8, 'ticket:reply', '客户工单官方答复与沟通'),
(8, 'ticket:close', '客户工单办结关闭处理');

-- 3. 补全商户主账号工单调度分派权限
INSERT INTO uam_role_grant (role_id, perm_code, perm_name) VALUES
(2, 'ticket:assign', '客户工单调度与经办客服分派')
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

-- 4. 新增顺丰商户客服专员演示账号 cs_sf / 123456
INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status, last_login_at, last_login_ip)
VALUES (10, 'cs_sf', '13800000010', 'cs@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), status = 1;

-- 5. 绑定账号与角色
DELETE FROM uam_account_role WHERE account_id = 10;
INSERT INTO uam_account_role (account_id, role_id) VALUES (10, 8);
