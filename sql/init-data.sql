-- ====================================================================
-- 翔云·低空智慧物流运营平台 基础种子数据脚本 (init-data.sql)
-- 包含：角色字典、权限授权、预置账户、商户档案、起降场站、无人机台账、空域网格与系统参数
-- 预置各角色默认密码均为：123456
-- ====================================================================

USE soarcloud_lae;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 初始化系统角色字典 (uam_role)
TRUNCATE TABLE uam_role;
INSERT INTO uam_role (
role_id, 
role_code, 
role_name, description) VALUES
(1, 'ROLE_ADMIN', '平台超级管理员', '拥有全平台治理、商户入驻审批、空域合规全局监管与敏感审计权限'),
(2, 'ROLE_MERCHANT', '低空物流商户', '低空货运企业主账号，拥有机队资产管理、订单接单调度、航线申报权限'),
(3, 'ROLE_ATC', '空管监察员', '空域合规监管部门，拥有飞行计划审批、空域动态管制与违规执法权限'),
(4, 'ROLE_PILOT', '无人机商用机长', '外场无人机执飞机长，拥有移动端起飞确认、在途状态上报与故障填报权限'),
(5, 'ROLE_MAINTAINER', '场站运维专员', '负责无人机巡检维保、智能充电机柜维护与场站预约确认'),
(6, 'ROLE_CUSTOMER', '货运个人/企业客户', '拥有在线下单试算、电子支付、轨迹实时查看与送达签收权限'),
(7, 'ROLE_DISPATCHER', '商户调度员', '负责订单综合看板、接单、调度航线规划与飞行计划申报');

-- 2. 初始化权限定义与角色授权 (uam_role_grant)
TRUNCATE TABLE uam_role_grant;
-- 管理员权限
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(1, 'auth:user:list', '账户与用户列表检索'),
(1, 'auth:user:freeze', '账户启停与冻结管理'),
(1, 'merchant:audit', '商户企业入驻审核'),
(1, 'customer:verify', '客户实名认证审核'),
(1, 'flight:approve', '飞行计划复核审批'),
(1, 'violation:manage', '空域违规调查与处罚'),
(1, 'sys:param:manage', '系统运行参数修改'),
(1, 'audit:log:view', '全量敏感操作日志审计');

-- 商户主账号权限
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(2, 'subaccount:manage', '商户企业子账号管理'),
(2, 'order:board:view', '商户订单看板与状态流转'),
(2, 'order:accept', '商户批量或单笔接单'),
(2, 'order:dispatch', '订单智能调度派单'),
(2, 'uav:manage', '机队资产台账与五态管理'),
(2, 'flight:apply', '航线规划与飞行计划申报'),
(2, 'site:booking', '起降场站机位占用预约'),
(2, 'settle:bill:view', '对账单管理与开票申请');

-- 商户调度员权限（无账单与组织权限）
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(7, 'order:board:view', '商户订单看板与状态流转'),
(7, 'order:accept', '商户批量或单笔接单'),
(7, 'order:dispatch', '订单智能调度派单'),
(7, 'flight:apply', '航线规划与飞行计划申报'),
(7, 'site:booking', '起降场站机位占用预约');


-- 空管权限
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(3, 'airspace:view', '空域三区态势总览'),
(3, 'airspace:manage', '临时管制区划定与下发'),
(3, 'flight:review', '飞行计划空间相交在线审批'),
(3, 'violation:punish', '下发违规行政处罚文书');

-- 机长权限
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(4, 'pilot:task:view', '移动端任务工作台'),
(4, 'pilot:flight:action', '起飞与在途确认操作'),
(4, 'fault:report', '外场设备故障紧急填报');

-- 客户权限
INSERT INTO uam_role_grant (
role_id, perm_code, perm_name) VALUES
(6, 'order:create', '在线寄件下单与运费试算'),
(6, 'order:track', '在途轨迹实时可视化跟踪'),
(6, 'order:confirm', '到件签收确认');

-- 3. 初始化预置系统账户 (uam_account) - 初始密码统一为 123456
-- 加盐散列使用 /.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
TRUNCATE TABLE uam_account;
INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status, last_login_at, last_login_ip) VALUES
(1, 'admin', '13800000001', 'admin@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3, 1, NOW(), '127.0.0.1'),
(2, 'merchant_sf', '13800000002', 'service@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1'),
(3, 'atc_officer', '13800000003', 'atc@caac-south.gov.cn', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 4, 1, NOW(), '127.0.0.1'),
(4, 'pilot_sf', '13800000004', 'pilot_sf@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 5, 1, NOW(), '127.0.0.1'),
(5, 'maintain_li', '13800000005', 'maintain_li@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 6, 1, NOW(), '127.0.0.1'),
(6, 'customer_demo', '13800000006', 'customer@tencent.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1, 1, NOW(), '127.0.0.1'),
(7, 'merchant_jd_sub', '13800000007', 'dispatch@jd-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1'),
(8, 'merchant_pending', '13800000008', 'biz@pending-cargo.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 0, NULL, NULL),
(9, 'dispatcher_sf', '13800000009', 'dispatcher@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1');

-- 4. 账户-角色多对多关联 (uam_account_role)
TRUNCATE TABLE uam_account_role;
INSERT INTO uam_account_role (account_id, 
role_id) VALUES
(1, 1), -- admin -> ROLE_ADMIN
(2, 2), -- merchant_sf -> ROLE_MERCHANT
(3, 3), -- atc_officer -> ROLE_ATC
(4, 4), -- pilot_sf -> ROLE_PILOT
(5, 5), -- maintain_li -> ROLE_MAINTAINER
(6, 6), -- customer_demo -> ROLE_CUSTOMER
(7, 2), -- merchant_jd_sub -> ROLE_MERCHANT
(8, 2), -- merchant_pending -> ROLE_MERCHANT
(9, 7); -- dispatcher_sf -> ROLE_DISPATCHER


-- 5. 初始化商户档案 (merchant_archive)
TRUNCATE TABLE merchant_archive;
INSERT INTO merchant_archive (merchant_id, account_id, corp_name, license_no, contact_name, contact_phone, airworthiness_cert, audit_state, audit_note, audit_at, auditor_id) VALUES
(1, 2, '深圳市丰翼低空物流科技有限公司', '91440300MA5EXXX123', '张经理', '13800000002', 'CAAC-CCAR92-2024-008', 1, '企业适航许可与空运承运资质完备，审核通过', NOW(), 1),
(2, 8, '翼联创新低空货运（深圳）有限公司', '91440300MA5EXXX999', '刘主管', '13800000008', 'CAAC-CCAR92-2024-P01', 0, '新商户入驻申请，待管理员资质审核', NULL, NULL);

-- 6. 初始化客户实名档案 (customer_archive)
TRUNCATE TABLE customer_archive;
INSERT INTO customer_archive (customer_id, account_id, 
real_name, id_card, credit_limit, auth_state) VALUES
(1, 6, '腾讯科技（深圳）低空物资部', '91440300708461136T', 50000.00, 1);

-- 7. 初始化起降场站与智能换电设施 (base_site) - 以深圳核心低空走廊为基准
TRUNCATE TABLE base_site;
INSERT INTO base_site (site_id, site_code, site_name, site_geo, capacity, occupied, site_state) VALUES
(1, 'SITE-SZ-001', '南山科技园低空综合起降枢纽', ST_GeomFromText('POINT(113.943 22.540)'), 6, 2, 1),
(2, 'SITE-SZ-002', '福田CBD深业上城楼顶起降场', ST_GeomFromText('POINT(114.062 22.553)'), 4, 1, 1),
(3, 'SITE-SZ-003', '龙岗大运智慧物流换电场站', ST_GeomFromText('POINT(114.218 22.695)'), 8, 3, 1),
(4, 'SITE-SZ-004', '盐田港综合保税区中转场站', ST_GeomFromText('POINT(114.265 22.578)'), 4, 0, 1);

-- 8. 初始化无人机资产台账 (uav_info)
TRUNCATE TABLE uav_info;
INSERT INTO uav_info (uav_id, uav_sn, model, merchant_id, max_load, max_range_km, battery_pct, health_score, fly_hours, state, home_site_id) VALUES
(1, 'UAV-FY90-001', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 92, 98, 142, 0, 1),
(2, 'UAV-FY90-002', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 85, 95, 110, 1, 1),
(3, 'UAV-FC30-001', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 100, 100, 45, 0, 2),
(4, 'UAV-FC30-002', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 40, 88, 260, 2, 2);

-- 8.1 初始化无人机维保与巡检工单 (uav_maintain)
TRUNCATE TABLE uav_maintain;
INSERT INTO uav_maintain (maintain_id, uav_id, maintain_type, maintain_state, fault_desc, repair_content, created_at) VALUES
(1, 4, 1, 1, '大疆FC30动力总成例行50小时巡检与桨叶动平衡调校', NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- 9. 初始化机组人员档案 (crew_staff)
TRUNCATE TABLE crew_staff;
INSERT INTO crew_staff (crew_id, account_id, crew_type, 
real_name, license_no, license_expire, fly_hours, status) VALUES
(1, 4, 1, '张志强', 'CAAC-UAV-20210988', '2028-12-31', 480, 1),
(2, 5, 2, '李维保', 'MAINT-CERT-2022011', '2027-10-30', 0, 1);

-- 10. 初始化空域管制区块网格 (aairspace_block)
TRUNCATE TABLE airspace_block;
-- 禁飞区1：深圳宝安机场净空保护禁飞区
INSERT INTO airspace_block (block_id, block_name, block_kind, geo_polygon, limit_alt, source) VALUES
(1, '深圳宝安国际机场核心净空禁飞区', 1,
 ST_GeomFromText('POLYGON((113.78 22.60, 113.84 22.60, 113.84 22.66, 113.78 22.66, 113.78 22.60))'), 0, '民航中南局');

-- 限飞区2：南山高新科技园核心限高管控区（限高120米）
INSERT INTO airspace_block (block_id, block_name, block_kind, geo_polygon, limit_alt, source) VALUES
(2, '南山科技园总部基地限高管制区', 2,
 ST_GeomFromText('POLYGON((113.93 22.52, 113.96 22.52, 113.96 22.56, 113.93 22.56, 113.93 22.52))'), 120, '市低空空管委');

-- 临时管制区3：大运中心近期重要活动临时管制（限高80米）
INSERT INTO airspace_block (block_id, block_name, block_kind, geo_polygon, limit_alt, effective_at, expire_at, source) VALUES
(3, '大运体育中心赛事临时管制空域', 3,
 ST_GeomFromText('POLYGON((114.20 22.68, 114.23 22.68, 114.23 22.71, 114.20 22.71, 114.20 22.68))'), 80,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), '公安低空警卫办');

-- 11. 初始化电子围栏规则 (fence_rule)
TRUNCATE TABLE fence_rule;
INSERT INTO fence_rule (
rule_id, 
rule_name, buffer_distance, max_altitude, action_policy) VALUES
(1, '重点敏感目标法定禁飞规则', 50, 0, 3),
(2, '城市建成区法定真高上限规则', 30, 120, 2),
(3, '沿海航道动态预警缓冲规则', 100, 150, 1);

-- 12. 初始化系统运行参数 (sys_param)
TRUNCATE TABLE sys_param;
INSERT INTO sys_param (param_id, param_group, param_key, param_value, param_desc) VALUES
(1, 'fee', 'fee.base_start', '25.00', '低空货运首重起步运费（元）'),
(2, 'fee', 'fee.per_km', '3.00', '超里程每公里单价（元/km）'),
(3, 'fee', 'fee.urgent_ratio', '1.30', '加急配送费率系数'),
(4, 'order', 'order.timeout_pay_minutes', '30', '订单待支付超时取消时间（分钟）'),
(5, 'order', 'order.timeout_accept_minutes', '60', '商户待接单超时取消时间（分钟）'),
(6, 'alarm', 'alarm.timeout_minutes', '120', '告警超时未处置预警时间（分钟）'),
(7, 'alarm', 'alarm.route_deviation_meters', '50', '在途偏航告警阈值（米）');

-- 13. 初始化外部接口网关健康登记 (api_endpoint)
TRUNCATE TABLE api_endpoint;
INSERT INTO api_endpoint (endpoint_id, service_name, endpoint_url, method, status_code, latency_ms, success_rate, check_time) VALUES
(1, '民航低空协同审批系统网关 (CAAC)', 'https://atc.soarcloud.com/gateway/flight/verify', 'POST', 200, 42, 99.98, NOW()),
(2, '高德地图Web API逆地理编码服务', 'https://restapi.amap.com/v3/geocode/regeo', 'GET', 200, 18, 100.00, NOW()),
(3, '中央气象台低空微气象与阵风预警', 'https://weather.soarcloud.com/api/v2/aviation-wind', 'GET', 200, 31, 99.85, NOW()),
(4, '中国银联 / 微信企业对账支付网关', 'https://api.mch.weixin.qq.com/v3/pay/transactions', 'POST', 200, 56, 100.00, NOW());
-- 14. 初始化飞行计划申报与违规案件数据
TRUNCATE TABLE flight_apply;
INSERT INTO flight_apply (apply_id, apply_sn, order_id, merchant_id, uav_id, pilot_id, route_line, alt_planned, start_at, end_at, check_state, approve_state, approver_id, approve_note, approve_at) VALUES
(1, 'FA202610170001', 3, 1, 2, 1, ST_GeomFromText('LINESTRING(113.943 22.540, 114.000 22.545, 114.062 22.553)'), 100, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1, 4, 3, '合规审批通过，机长已确认起飞进入在途执行中', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(2, 'FA202610170000', 4, 1, 1, 1, ST_GeomFromText('LINESTRING(114.218 22.695, 114.240 22.690, 114.265 22.578)'), 90, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 1, 5, 3, '飞行任务已圆满完成送达', DATE_SUB(NOW(), INTERVAL 3 HOUR));

TRUNCATE TABLE violation_case;
INSERT INTO violation_case (case_id, case_sn, apply_id, violate_kind, level, punish_state, evidence, handled_by, handled_at) VALUES 
(1, 'VC202610170001', 1, 1, 1, 2, '雷达实测轨迹偏离规划中轴线达128米，持续42秒，责令留痕警告', 3, NOW()),
(2, 'VC202610170002', 1, 2, 3, 0, '非法闯入宝安机场外围净空保护缓冲区深达160米，雷达已记录存证', NULL, NULL),
(3, 'VC202610170003', 1, 3, 2, 0, '南山科技园管制区巡航高度达165米（超出法定限高120米达45米）', NULL, NULL),
(4, 'VC202610170004', 1, 4, 1, 1, '大运中心降落超时占用起降航道35分钟，造成后续机组盘旋等待', NULL, NULL);

-- 15. 初始化货运订单主表 (cargo_order)
TRUNCATE TABLE cargo_order;
INSERT INTO cargo_order (order_id, order_sn, customer_id, merchant_id, sender_addr, receiver_addr, sender_geo, receiver_geo, goods_type, goods_weight, urgent_flag, fee_base, fee_airspace, fee_urgent, fee_discount, fee_total, pay_way, pay_state, order_state, uav_id, apply_id, expect_arrive_at) VALUES
(1, 'ORD202610170001', 1, 1, '南山科技园低空枢纽（深圳湾）', '深圳大学总医院急诊楼起降坪', ST_GeomFromText('POINT(113.952 22.532)'), ST_GeomFromText('POINT(113.987 22.592)'), 2, 5.50, 1, 45.00, 25.00, 21.00, 3.00, 88.00, 1, 1, 20, NULL, NULL, DATE_ADD(NOW(), INTERVAL 2 HOUR)),
(2, 'ORD202610170002', 1, 1, '南山科技园枢纽站A区', '福田CBD深业上城楼顶起降场', ST_GeomFromText('POINT(113.943 22.540)'), ST_GeomFromText('POINT(114.062 22.553)'), 3, 12.00, 0, 42.00, 28.50, 0.00, 5.00, 65.50, 1, 1, 30, NULL, NULL, DATE_ADD(NOW(), INTERVAL 3 HOUR)),
(3, 'ORD202610170003', 1, 1, '南山科技园枢纽站B区', '福田CBD深业上城楼顶起降场', ST_GeomFromText('POINT(113.943 22.540)'), ST_GeomFromText('POINT(114.062 22.553)'), 4, 8.00, 1, 55.00, 30.00, 25.00, 0.00, 110.00, 1, 1, 50, 2, 1, DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
(4, 'ORD202610170004', 1, 1, '龙岗大运智慧物流场站', '盐田港综合保税区中转站', ST_GeomFromText('POINT(114.218 22.695)'), ST_GeomFromText('POINT(114.265 22.578)'), 1, 1.50, 0, 25.00, 15.00, 0.00, 5.00, 35.00, 1, 1, 60, 1, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- 16. 初始化订单状态流水表 (order_state_flow)
TRUNCATE TABLE order_state_flow;
INSERT INTO order_state_flow (flow_id, order_id, from_state, to_state, operator_id, remark) VALUES
(1, 1, 10, 20, 6, '客户完成在线微信支付'),
(2, 2, 10, 20, 6, '客户在线支付成功'),
(3, 2, 20, 30, 2, '商户调度中心确认承接该运单'),
(4, 3, 10, 20, 6, '客户在线支付成功'),
(5, 3, 20, 30, 2, '商户确认接单进入调度队列'),
(6, 3, 30, 40, 2, '调度员指派无人机UAV-FY90-002并联动申报飞行计划'),
(7, 3, 40, 50, 4, '机长完成航前巡检并确认起飞执行在途运输，执飞机号UAV-FY90-002'),
(8, 4, 10, 20, 6, '客户在线支付成功'),
(9, 4, 20, 30, 2, '商户接单'),
(10, 4, 30, 40, 2, '指派无人机UAV-FY90-001完成调度'),
(11, 4, 40, 50, 4, '起飞执行'),
(12, 4, 50, 60, 4, '已安全精准降落目的地，客户签收完成，设备恢复闲置');

-- 17. 初始化货运结算对账单表 (settle_bill)
TRUNCATE TABLE settle_bill;
INSERT INTO settle_bill (bill_id, bill_sn, order_id, merchant_id, amount, bill_state, invoice_flag, settle_at) VALUES
(1, 'BILL202610170001', 4, 1, 35.00, 1, 0, NOW());

-- 18. 初始化在途飞行航迹明细 (fly_trace) - 为当前执行中的任务1预置在途航迹点
TRUNCATE TABLE fly_trace;
INSERT INTO fly_trace (trace_id, apply_id, uav_id, point_geo, altitude, speed, battery_remain, report_time, created_at) VALUES
(1, 1, 2, ST_GeomFromText('POINT(113.943000 22.540000)'), 100.00, 12.50, 95, DATE_SUB(NOW(), INTERVAL 15 MINUTE), NOW()),
(2, 1, 2, ST_GeomFromText('POINT(113.955000 22.541000)'), 102.00, 12.80, 94, DATE_SUB(NOW(), INTERVAL 12 MINUTE), NOW()),
(3, 1, 2, ST_GeomFromText('POINT(113.970000 22.542500)'), 101.50, 13.00, 93, DATE_SUB(NOW(), INTERVAL 9 MINUTE), NOW()),
(4, 1, 2, ST_GeomFromText('POINT(113.985000 22.543800)'), 100.00, 12.60, 92, DATE_SUB(NOW(), INTERVAL 6 MINUTE), NOW()),
(5, 1, 2, ST_GeomFromText('POINT(114.000000 22.545000)'), 103.00, 12.40, 91, DATE_SUB(NOW(), INTERVAL 3 MINUTE), NOW()),
(6, 1, 2, ST_GeomFromText('POINT(114.015000 22.546500)'), 102.00, 12.50, 90, NOW(), NOW());

SET FOREIGN_KEY_CHECKS = 1;
