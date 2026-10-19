-- H2 测试种子数据
INSERT INTO uam_role (role_id, role_code, role_name, description) VALUES
(1, 'ROLE_ADMIN', '平台超级管理员', '拥有全平台治理权限'),
(2, 'ROLE_MERCHANT', '低空物流商户', '低空货运企业主账号'),
(3, 'ROLE_ATC', '空管监察员', '空域合规监管部门'),
(4, 'ROLE_PILOT', '无人机商用机长', '外场无人机执飞机长'),
(5, 'ROLE_MAINTAINER', '场站运维专员', '负责无人机巡检维保'),
(6, 'ROLE_CUSTOMER', '货运个人/企业客户', '拥有在线下单试算权限'),
(7, 'ROLE_DISPATCHER', '商户调度员', '负责订单调度、航线规划与飞行计划申报'),
(8, 'ROLE_CS', '商户客服专员', '负责客户工单的受理、协同跟进、答复与办结关闭');

INSERT INTO uam_account (account_id, login_name, mobile, email, pwd_digest, account_kind, status) VALUES
(1, 'admin', '13800000001', 'admin@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3, 1),
(2, 'merchant_sf', '13800000002', 'service@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(3, 'atc_officer', '13800000003', 'atc@caac-south.gov.cn', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 4, 1),
(4, 'pilot_sf', '13800000004', 'pilot_sf@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 5, 1),
(5, 'maintain_li', '13800000005', 'maintain_li@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 6, 1),
(6, 'customer_demo', '13800000006', 'customer@tencent.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1, 1),
(7, 'merchant_jd_sub', '13800000007', 'dispatch@jd-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(8, 'merchant_pending', '13800000008', 'biz@pending-cargo.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 0),
(9, 'dispatcher_sf', '13800000009', 'dispatcher@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1),
(10, 'cs_sf', '13800000010', 'cs@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1);

INSERT INTO uam_account_role (account_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 2),
(8, 2),
(9, 7),
(10, 8);


INSERT INTO merchant_archive (merchant_id, account_id, corp_name, license_no, contact_name, contact_phone, airworthiness_cert, audit_state, audit_note) VALUES
(1, 2, '深圳市丰翼低空物流科技有限公司', '91440300MA5EXXX123', '张经理', '13800000002', 'CAAC-CCAR92-2024-008', 1, '企业适航许可与空运承运资质完备，审核通过'),
(2, 8, '翼联创新低空货运（深圳）有限公司', '91440300MA5EXXX999', '刘主管', '13800000008', 'CAAC-CCAR92-2024-P01', 0, '新商户入驻申请，待管理员资质审核');

INSERT INTO customer_archive (customer_id, account_id, real_name, id_card, credit_limit, auth_state) VALUES
(1, 6, '腾讯科技（深圳）低空物资部', '91440300708461136T', 50000.00, 1);

-- 场站数据 (base_site)
INSERT INTO base_site (site_id, site_code, site_name, site_geo, capacity, occupied, site_state) VALUES
(1, 'SITE-SZ-001', '南山科技园低空综合起降枢纽', '113.943,22.540', 6, 2, 1),
(2, 'SITE-SZ-002', '福田CBD深业上城楼顶起降场', '114.062,22.553', 4, 1, 1),
(3, 'SITE-SZ-003', '龙岗大运智慧物流换电场站', '114.218,22.695', 8, 3, 1),
(4, 'SITE-SZ-004', '盐田港综合保税区中转场站', '114.265,22.578', 4, 0, 1);

-- 客户常用地址簿数据
INSERT INTO customer_address (address_id, customer_id, contact_name, contact_phone, province, city, district, street, lng, lat, detail, is_default) VALUES
(1, 6, '陈总监', '13800000006', '广东省', '深圳市', '南山区', '粤海街道', 113.943000, 22.540000, '科技南十二路28号康佳研发大厦A座18层低空接驳站', 1),
(2, 6, '李经理', '13800000066', '广东省', '深圳市', '福田区', '莲花街道', 114.062000, 22.553000, '皇岗路5001号深业上城南区B座顶层停机坪', 0);


-- 无人机台账 (uav_info)
INSERT INTO uav_info (uav_id, uav_sn, model, merchant_id, max_load, max_range_km, battery_pct, health_score, fly_hours, state, home_site_id) VALUES
(1, 'UAV-FY90-001', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 92, 98, 142, 0, 1),
(2, 'UAV-FY90-002', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 85, 95, 110, 1, 1),
(3, 'UAV-FC30-001', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 100, 100, 45, 0, 2),
(4, 'UAV-FC30-002', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 40, 88, 260, 2, 2);

-- 维保记录 (uav_maintain)
INSERT INTO uav_maintain (maintain_id, uav_id, maintain_type, maintain_state, fault_desc, repair_content, created_at) VALUES
(1, 4, 1, 1, '大疆FC30动力总成例行50小时巡检与桨叶动平衡调校', NULL, DATEADD('HOUR', -2, CURRENT_TIMESTAMP));

-- 机组人员档案 (crew_staff)
INSERT INTO crew_staff (crew_id, account_id, crew_type, real_name, license_no, license_expire, fly_hours, status) VALUES
(1, 4, 1, '张志强', 'CAAC-UAV-20210988', '2028-12-31', 480, 1),
(2, 5, 2, '李维保', 'MAINT-CERT-2022011', '2027-10-30', 0, 1),
(3, 7, 1, '陈立军', 'CAAC-UAV-20230566', '2029-05-31', 360, 1);


-- 系统参数 (sys_param)
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
(1, 'FA202610170001', 101, 1, 1, 1, 'LINESTRING(113.943 22.540, 114.000 22.545, 114.062 22.553)', 100, '2026-10-17 09:00:00', '2026-10-17 10:30:00', 1, 1, 3, '合规校验通过，航线无空域冲突，批准执行', '2026-10-17 12:00:00'),
(2, 'FA202610170002', 102, 1, 3, 1, 'LINESTRING(114.218 22.695, 114.240 22.690, 114.265 22.578)', 90, '2026-10-19 14:00:00', '2026-10-19 15:30:00', 1, 0, NULL, NULL, NULL);

-- 样例违规案件 (violation_case)
INSERT INTO violation_case (case_id, case_sn, apply_id, violate_kind, level, punish_state, evidence, handled_by, handled_at) VALUES
(1, 'VC202610170001', 1, 1, 1, 2, '雷达实测轨迹偏离规划中轴线达128米，持续42秒，责令留痕警告', 3, CURRENT_TIMESTAMP),
(2, 'VC202610170002', 1, 2, 3, 0, '非法闯入宝安机场外围净空保护缓冲区深达160米，雷达已记录存证', NULL, NULL),
(3, 'VC202610170003', 1, 3, 2, 0, '南山科技园管制区巡航高度达165米（超出法定限高120米达45米）', NULL, NULL),
(4, 'VC202610170004', 1, 4, 1, 1, '大运中心降落超时占用起降航道35分钟，造成后续机组盘旋等待', NULL, NULL);

