-- 角色初始化
INSERT INTO uam_role (role_id, role_code, role_name, description) VALUES
(1, 'ROLE_ADMIN', '平台超级管理员', '拥有全平台治理与审批权限'),
(2, 'ROLE_MERCHANT', '低空物流商户', '低空物流商户主账号，管理机队与订单'),
(3, 'ROLE_ATC', '空管监察员', '空域合规监管与飞行计划审批'),
(4, 'ROLE_PILOT', '无人机商用机长', '外场无人机执飞作业'),
(5, 'ROLE_MAINTAINER', '场站运维专员', '场站设施与设备维保'),
(6, 'ROLE_CUSTOMER', '货运客户', '在线寄件下单'),
(7, 'ROLE_DISPATCHER', '商户调度员', '负责订单调度、航线规划与飞行计划申报'),
(8, 'ROLE_CS', '商户客服专员', '负责客户工单的受理、协同跟进与办结关闭');

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
(12, 6, 'order:create', '下单寄件'),
(13, 7, 'order:manage', '订单管理'),
(14, 7, 'flight:apply', '飞行计划申报'),
(15, 8, 'ticket:reply', '工单答复与关闭');

-- 账户数据（默认密码均为 123456）
-- BCrypt 散列：$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status) VALUES
(1, 'admin', '13800000001', 'admin@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3, 1),
(2, 'merchant_sf', '13800000002', 'sf@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(3, 'atc_officer', '13800000003', 'atc@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 4, 1),
(4, 'pilot_sf', '13800000004', 'pilot@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 5, 1),
(5, 'maintain_li', '13800000005', 'maintain@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 6, 1),
(6, 'customer_demo', '13800000006', 'customer@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1, 1),
(7, 'merchant_sf_sub1', '13800000007', 'sub1@sf.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(8, 'merchant_pending', '13800000008', 'pending@soarcloud.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 0),
(9, 'dispatcher_sf', '13800000009', 'dispatcher@sf-air.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(10, 'cs_sf', '13800000010', 'cs@sf-air.com', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1);


-- 账户角色关联
INSERT INTO uam_account_role (id, account_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 5),
(6, 6, 6),
(7, 7, 2),
(8, 8, 2),
(9, 9, 7),
(10, 10, 8);

-- 商户企业档案
INSERT INTO merchant_archive (merchant_id, account_id, corp_name, license_no, contact_name, contact_phone, airworthiness_cert, audit_state, audit_note) VALUES
(1, 2, '深圳市丰翼低空物流科技有限公司', '91440300MA5EXXX123', '张经理', '13800000002', 'CAAC-CCAR92-2024-008', 1, '企业适航许可与承运资质齐全，已审核通过'),
(2, 8, '翼联创新低空货运（深圳）有限公司', '91440300MA5EXXX999', '刘主管', '13800000008', 'CAAC-CCAR92-2024-P01', 0, '新入驻申请待审核');

-- 客户实名档案
INSERT INTO customer_archive (customer_id, account_id, real_name, id_card, credit_limit, auth_state) VALUES
(1, 6, '腾讯科技（深圳）低空物资部', '91440300708461136T', 50000.00, 1);

-- 系统参数
INSERT INTO sys_param (param_id, param_group, param_key, param_value, param_desc) VALUES
(1, 'fee', 'fee.base_start', '25.00', '低空货运首重起步运费（元）'),
(2, 'fee', 'fee.per_km', '3.00', '超里程每公里单价（元/km）'),
(3, 'fee', 'fee.urgent_ratio', '1.30', '加急配送费率系数'),
(4, 'order', 'order.timeout_pay_minutes', '30', '订单待支付超时取消时间（分钟）'),
(5, 'order', 'order.timeout_accept_minutes', '60', '商户待接单超时取消时间（分钟）'),
(6, 'alarm', 'alarm.timeout_minutes', '120', '告警超时未处置预警时间（分钟）'),
(7, 'alarm', 'alarm.route_deviation_meters', '50', '在途偏航告警阈值（米）');

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
(1, 'FA202610170001', 3, 1, 1, 1, 'LINESTRING(113.943 22.540, 114.000 22.545, 114.062 22.553)', 100, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 1, 4, 3, '合规审批通过，机长已确认起飞进入在途执行中', DATEADD('MINUTE', -30, CURRENT_TIMESTAMP)),
(2, 'FA202610170000', 4, 1, 2, 1, 'LINESTRING(114.218 22.695, 114.240 22.690, 114.265 22.578)', 90, DATEADD('HOUR', -3, CURRENT_TIMESTAMP), DATEADD('HOUR', -1, CURRENT_TIMESTAMP), 1, 5, 3, '飞行任务已圆满完成送达', DATEADD('HOUR', -3, CURRENT_TIMESTAMP));

-- 样例违规案件 (violation_case)
INSERT INTO violation_case (case_id, case_sn, apply_id, violate_kind, level, punish_state, evidence, handled_by, handled_at) VALUES
(1, 'VC202610170001', 1, 1, 1, 2, '雷达实测轨迹偏离规划中轴线达128米，持续42秒，责令留痕警告', 3, CURRENT_TIMESTAMP),
(2, 'VC202610170002', 1, 2, 3, 0, '非法闯入宝安机场外围净空保护缓冲区深达160米，雷达已记录存证', NULL, NULL),
(3, 'VC202610170003', 1, 3, 2, 0, '南山科技园管制区巡航高度达165米（超出法定限高120米达45米）', NULL, NULL),
(4, 'VC202610170004', 1, 4, 1, 1, '大运中心降落超时占用起降航道35分钟，造成后续机组盘旋等待', NULL, NULL);


-- 货运订单初始化 (cargo_order)
INSERT INTO cargo_order (order_id, order_sn, customer_id, merchant_id, sender_addr, receiver_addr, sender_geo, receiver_geo, goods_type, goods_weight, urgent_flag, fee_base, fee_airspace, fee_urgent, fee_discount, fee_total, pay_way, pay_state, order_state, uav_id, apply_id, expect_arrive_at) VALUES
(1, 'ORD202610170001', 1, 1, '南山科技园低空枢纽（深圳湾）', '深圳大学总医院急诊楼起降坪', 'POINT(113.952 22.532)', 'POINT(113.987 22.592)', 2, 5.50, 1, 45.00, 25.00, 21.00, 3.00, 88.00, 1, 1, 20, NULL, NULL, DATEADD('HOUR', 2, CURRENT_TIMESTAMP)),
(2, 'ORD202610170002', 1, 1, '南山科技园枢纽站A区', '福田CBD深业上城楼顶起降场', 'POINT(113.943 22.540)', 'POINT(114.062 22.553)', 3, 12.00, 0, 42.00, 28.50, 0.00, 5.00, 65.50, 1, 1, 30, NULL, NULL, DATEADD('HOUR', 3, CURRENT_TIMESTAMP)),
(3, 'ORD202610170003', 1, 1, '南山科技园枢纽站B区', '福田CBD深业上城楼顶起降场', 'POINT(113.943 22.540)', 'POINT(114.062 22.553)', 4, 8.00, 1, 55.00, 30.00, 25.00, 0.00, 110.00, 1, 1, 50, 1, 1, DATEADD('MINUTE', 30, CURRENT_TIMESTAMP)),
(4, 'ORD202610170004', 1, 1, '龙岗大运智慧物流场站', '盐田港综合保税区中转站', 'POINT(114.218 22.695)', 'POINT(114.265 22.578)', 1, 1.50, 0, 25.00, 15.00, 0.00, 5.00, 35.00, 1, 1, 60, 2, 2, DATEADD('HOUR', -1, CURRENT_TIMESTAMP));

-- 订单状态流水表 (order_state_flow)
INSERT INTO order_state_flow (flow_id, order_id, from_state, to_state, operator_id, remark) VALUES
(1, 1, 10, 20, 6, '客户完成在线微信支付'),
(2, 2, 10, 20, 6, '客户在线支付成功'),
(3, 2, 20, 30, 2, '商户调度中心确认承接该运单'),
(4, 3, 10, 20, 6, '客户在线支付成功'),
(5, 3, 20, 30, 2, '商户确认接单进入调度队列'),
(6, 3, 30, 40, 2, '调度员指派无人机UAV-FY90-001并联动申报飞行计划'),
(7, 3, 40, 50, 4, '机长完成航前巡检并确认起飞执行在途运输'),
(8, 4, 10, 20, 6, '客户在线支付成功'),
(9, 4, 20, 30, 2, '商户接单'),
(10, 4, 30, 40, 2, '指派无人机完成调度'),
(11, 4, 40, 50, 4, '起飞执行'),
(12, 4, 50, 60, 4, '已安全精准降落目的地，客户签收完成');

-- 结算账单表 (settle_bill)
INSERT INTO settle_bill (bill_id, bill_sn, order_id, merchant_id, amount, bill_state, invoice_flag, settle_at) VALUES
(1, 'BILL202610170001', 4, 1, 35.00, 1, 0, CURRENT_TIMESTAMP);

-- 飞行航迹明细 (fly_trace)
INSERT INTO fly_trace (trace_id, apply_id, uav_id, point_geo, altitude, speed, battery_remain, report_time, created_at) VALUES
(1, 1, 1, 'POINT(113.943000 22.540000)', 100.00, 12.50, 95, DATEADD('MINUTE', -15, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(2, 1, 1, 'POINT(113.955000 22.541000)', 102.00, 12.80, 94, DATEADD('MINUTE', -12, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(3, 1, 1, 'POINT(113.970000 22.542500)', 101.50, 13.00, 93, DATEADD('MINUTE', -9, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(4, 1, 1, 'POINT(113.985000 22.543800)', 100.00, 12.60, 92, DATEADD('MINUTE', -6, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(5, 1, 1, 'POINT(114.000000 22.545000)', 103.00, 12.40, 91, DATEADD('MINUTE', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
(6, 1, 1, 'POINT(114.015000 22.546500)', 102.00, 12.50, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 客户常用地址簿数据
INSERT INTO customer_address (address_id, customer_id, contact_name, contact_phone, province, city, district, street, lng, lat, detail, is_default) VALUES
(1, 6, '陈总监', '13800000006', '广东省', '深圳市', '南山区', '粤海街道', 113.943000, 22.540000, '科技南十二路28号康佳研发大厦A座18层低空接驳站', 1),
(2, 6, '李经理', '13800000066', '广东省', '深圳市', '福田区', '莲花街道', 114.062000, 22.553000, '皇岗路5001号深业上城南区B座顶层停机坪', 0);



