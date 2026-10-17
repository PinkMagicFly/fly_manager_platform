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

-- 空域管控区块网格 (airspace_block)
INSERT INTO airspace_block (block_id, block_name, block_kind, geo_polygon, limit_alt, effective_at, expire_at, source) VALUES
(1, '深圳宝安国际机场核心净空禁飞区', 1, 'POLYGON((113.78 22.60, 113.84 22.60, 113.84 22.66, 113.78 22.66, 113.78 22.60))', 0, NULL, NULL, '民航中南局'),
(2, '南山科技园总部基地限高管制区', 2, 'POLYGON((113.93 22.52, 113.96 22.52, 113.96 22.56, 113.93 22.56, 113.93 22.52))', 120, NULL, NULL, '市低空空管委'),
(3, '大运体育中心赛事临时管制空域', 3, 'POLYGON((114.20 22.68, 114.23 22.68, 114.23 22.71, 114.20 22.71, 114.20 22.68))', 80, DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', 7, CURRENT_TIMESTAMP), '公安低空警卫办');

-- 电子围栏规则 (fence_rule)
INSERT INTO fence_rule (rule_id, rule_name, buffer_distance, max_altitude, action_policy) VALUES
(1, '重点敏感目标法定禁飞规则', 50, 0, 3),
(2, '城市建成区法定真高上限规则', 30, 120, 2),
(3, '沿海航道动态预警缓冲规则', 100, 150, 1);

-- 样例申报单 (flight_apply)
INSERT INTO flight_apply (apply_id, apply_sn, order_id, merchant_id, uav_id, pilot_id, route_line, alt_planned, start_at, end_at, check_state, approve_state, approver_id, approve_note, approve_at) VALUES
(1, 'FA202610170001', 101, 1, 1, 1, 'LINESTRING(113.943 22.540, 114.000 22.545, 114.062 22.553)', 100, '2026-10-18 09:00:00', '2026-10-18 10:30:00', 1, 1, 3, '合规校验通过，航线无空域冲突，批准执行', '2026-10-17 12:00:00'),
(2, 'FA202610170002', 102, 1, 3, 1, 'LINESTRING(114.218 22.695, 114.240 22.690, 114.265 22.578)', 90, '2026-10-19 14:00:00', '2026-10-19 15:30:00', 1, 0, NULL, NULL, NULL);

-- 样例违规案件 (violation_case)
INSERT INTO violation_case (case_id, case_sn, apply_id, violate_kind, level, punish_state, evidence, handled_by, handled_at) VALUES
(1, 'VC202610170001', 1, 1, 1, 2, '雷达实测轨迹偏离规划中轴线达128米，持续42秒，责令留痕警告', 3, CURRENT_TIMESTAMP),
(2, 'VC202610170002', 1, 2, 3, 0, '非法闯入宝安机场外围净空保护缓冲区深达160米，雷达已记录存证', NULL, NULL),
(3, 'VC202610170003', 1, 3, 2, 0, '南山科技园管制区巡航高度达165米（超出法定限高120米达45米）', NULL, NULL),
(4, 'VC202610170004', 1, 4, 1, 1, '大运中心降落超时占用起降航道35分钟，造成后续机组盘旋等待', NULL, NULL);

