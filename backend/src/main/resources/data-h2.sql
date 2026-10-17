-- 角色初始化
INSERT INTO uam_role (role_id, role_code, role_name, description) VALUES
(1, 'ROLE_ADMIN', '平台超级管理员', '拥有全平台治理与审批权限'),
(2, 'ROLE_MERCHANT', '低空物流商户', '低空物流商户主账号，管理机队与订单'),
(3, 'ROLE_ATC', '空管监察员', '空域合规监管与飞行计划审批'),
(4, 'ROLE_PILOT', '无人机商用机长', '外场无人机执飞作业'),
(5, 'ROLE_MAINTAINER', '场站运维专员', '场站设施与设备维保'),
(6, 'ROLE_CUSTOMER', '货运客户', '在线寄件下单');

-- 权限字典
INSERT INTO uam_role_grant (grant_id, role_id, perm_code, perm_name) VALUES
(1, 1, 'auth:user:list', '账户列表检索'),
(2, 1, 'auth:user:freeze', '账户启停管理'),
(3, 1, 'merchant:audit', '商户资质审核'),
(4, 1, 'customer:verify', '客户实名认证审核'),
(5, 1, 'audit:log:view', '全量操作日志审计'),
(6, 2, 'subaccount:manage', '企业子账号管理'),
(7, 2, 'order:manage', '订单管理'),
(8, 2, 'flight:apply', '飞行计划申报'),
(9, 3, 'airspace:view', '空域态势查看'),
(10, 3, 'flight:review', '飞行计划审批'),
(11, 4, 'pilot:flight', '起飞与降落操作'),
(12, 6, 'order:create', '下单寄件');

-- 账户数据（默认密码均为 123456）
-- BCrypt 散列：$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status) VALUES
(1, 'admin', '13800000001', 'admin@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3, 1),
(2, 'merchant_sf', '13800000002', 'sf@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(3, 'atc_officer', '13800000003', 'atc@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 4, 1),
(4, 'pilot_zhang', '13800000004', 'pilot@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 5, 1),
(5, 'maintain_li', '13800000005', 'maintain@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 6, 1),
(6, 'customer_demo', '13800000006', 'customer@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1, 1),
(7, 'merchant_sf_sub1', '13800000007', 'sub1@sf.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(8, 'merchant_pending', '13800000008', 'pending@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 0);

-- 账户角色关联
INSERT INTO uam_account_role (id, account_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5),
(6, 6, 6),
(7, 7, 2),
(8, 8, 2);

-- 商户企业档案
INSERT INTO merchant_archive (merchant_id, account_id, corp_name, license_no, contact_name, contact_phone, airworthiness_cert, audit_state, audit_note) VALUES
(1, 2, '深圳市丰翼低空物流科技有限公司', '91440300MA5EXXX123', '张经理', '13800000002', 'CAAC-CCAR92-2024-008', 1, '企业适航许可与承运资质齐全，已审核通过'),
(2, 8, '翼联创新低空货运（深圳）有限公司', '91440300MA5EXXX999', '刘主管', '13800000008', 'CAAC-CCAR92-2024-P01', 0, '新入驻申请待审核');

-- 客户实名档案
INSERT INTO customer_archive (customer_id, account_id, real_name, id_card, credit_limit, auth_state) VALUES
(1, 6, '腾讯科技（深圳）低空物资部', '91440300708461136T', 50000.00, 1);

-- 系统参数
INSERT INTO sys_param (param_id, param_group, param_key, param_value, param_desc) VALUES
(1, 'fee_rule', 'BASE_FREIGHT_PRICE', '35.00', '低空货运首重起步运费（元）'),
(2, 'alarm_threshold', 'ROUTE_DEVIATION_METERS', '50', '允许最大偏航米数');
