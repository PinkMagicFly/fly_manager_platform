-- ====================================================================
-- 翔云·低空智慧物流运营平台 数据库补丁脚本 (阶段 5A.1)
-- 包含：角色字典扩充（商户调度员 ROLE_DISPATCHER）、调度员与机长账号规范
-- ====================================================================

USE soarcloud_lae;
SET NAMES utf8mb4;

-- 1. 扩充角色字典：商户调度员 (ROLE_DISPATCHER)
INSERT INTO uam_role (role_id, role_code, role_name, description)
VALUES (7, 'ROLE_DISPATCHER', '商户调度员', '负责订单综合看板、接单、调度航线规划与飞行计划申报')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- 2. 授权商户调度员权限（订单看板、接单、调度、申报、场站预约，无账单与组织权限）
DELETE FROM uam_role_grant WHERE role_id = 7;
INSERT INTO uam_role_grant (role_id, perm_code, perm_name) VALUES
(7, 'order:board:view', '商户订单看板与状态流转'),
(7, 'order:accept', '商户批量或单笔接单'),
(7, 'order:dispatch', '订单智能调度派单'),
(7, 'flight:apply', '航线规划与飞行计划申报'),
(7, 'site:booking', '起降场站机位占用预约');

-- 3. 规范机长与调度员预置账户
-- 将 pilot_zhang 规范更名为 pilot_sf
UPDATE uam_account SET login_name = 'pilot_sf', email = 'pilot_sf@sf-air.com' WHERE account_id = 4;

-- 新增顺丰商户调度员账号 dispatcher_sf / 123456
INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status, last_login_at, last_login_ip)
VALUES (9, 'dispatcher_sf', '13800000009', 'dispatcher@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), status = 1;

-- 绑定账号角色
DELETE FROM uam_account_role WHERE account_id = 9;
INSERT INTO uam_account_role (account_id, role_id) VALUES (9, 7);
