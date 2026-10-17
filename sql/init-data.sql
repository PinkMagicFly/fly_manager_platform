-- ====================================================================
-- 翔云·低空智慧物流运营平台 基础种子数据脚本 (init-data.sql)
-- 包含：角色字典、权限授权、预置账户、商户档案、起降场站、无人机台账、空域网格与系统参数
-- 预置各角色默认密码均为：123456
-- ====================================================================

USE soarcloud_lae;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 初始化系统角色字典 (uam_role)
TRUNCATE TABLE uam_role;
INSERT INTO uam_role (
ole_id, 
ole_code, 
ole_name, description) VALUES
(1, 'ROLE_ADMIN', '平台超级管理员', '拥有全平台治理、商户入驻审批、空域合规全局监管与敏感审计权限'),
(2, 'ROLE_MERCHANT', '低空物流商户', '低空货运企业主账号，拥有机队资产管理、订单接单调度、航线申报权限'),
(3, 'ROLE_ATC', '空管监察员', '空域合规监管部门，拥有飞行计划审批、空域动态管制与违规执法权限'),
(4, 'ROLE_PILOT', '无人机商用机长', '外场无人机执飞机长，拥有移动端起飞确认、在途状态上报与故障填报权限'),
(5, 'ROLE_MAINTAINER', '场站运维专员', '负责无人机巡检维保、智能充电机柜维护与场站预约确认'),
(6, 'ROLE_CUSTOMER', '货运个人/企业客户', '拥有在线下单试算、电子支付、轨迹实时查看与送达签收权限');

-- 2. 初始化权限定义与角色授权 (uam_role_grant)
TRUNCATE TABLE uam_role_grant;
-- 管理员权限
INSERT INTO uam_role_grant (
ole_id, perm_code, perm_name) VALUES
(1, 'auth:user:list', '账户与用户列表检索'),
(1, 'auth:user:freeze', '账户启停与冻结管理'),
(1, 'merchant:audit', '商户企业入驻审核'),
(1, 'customer:verify', '客户实名认证审核'),
(1, 'flight:approve', '飞行计划复核审批'),
(1, 'violation:manage', '空域违规调查与处罚'),
(1, 'sys:param:manage', '系统运行参数修改'),
(1, 'audit:log:view', '全量敏感操作日志审计');

-- 商户权限
INSERT INTO uam_role_grant (
ole_id, perm_code, perm_name) VALUES
(2, 'subaccount:manage', '商户企业子账号管理'),
(2, 'order:board:view', '商户订单看板与状态流转'),
(2, 'order:accept', '商户批量或单笔接单'),
(2, 'order:dispatch', '订单智能调度派单'),
(2, 'uav:manage', '机队资产台账与五态管理'),
(2, 'flight:apply', '航线规划与飞行计划申报'),
(2, 'site:booking', '起降场站机位占用预约'),
(2, 'settle:bill:view', '对账单管理与开票申请');

-- 空管权限
INSERT INTO uam_role_grant (
ole_id, perm_code, perm_name) VALUES
(3, 'airspace:view', '空域三区态势总览'),
(3, 'airspace:manage', '临时管制区划定与下发'),
(3, 'flight:review', '飞行计划空间相交在线审批'),
(3, 'violation:punish', '下发违规行政处罚文书');

-- 机长权限
INSERT INTO uam_role_grant (
ole_id, perm_code, perm_name) VALUES
(4, 'pilot:task:view', '移动端任务工作台'),
(4, 'pilot:flight:action', '起飞与在途确认操作'),
(4, 'fault:report', '外场设备故障紧急填报');

-- 客户权限
INSERT INTO uam_role_grant (
ole_id, perm_code, perm_name) VALUES
(6, 'order:create', '在线寄件下单与运费试算'),
(6, 'order:track', '在途轨迹实时可视化跟踪'),
(6, 'order:confirm', '到件签收确认');

-- 3. 初始化预置系统账户 (uam_account) - 初始密码统一为 123456
-- 加盐散列使用 /.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
TRUNCATE TABLE uam_account;
INSERT INTO uam_account (ccount_id, login_name, mobile, email, pwd_digest, ccount_kind, status, last_login_at, last_login_ip) VALUES
(1, 'admin', '13800000001', 'admin@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3, 1, NOW(), '127.0.0.1'),
(2, 'merchant_sf', '13800000002', 'service@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1'),
(3, 'atc_officer', '13800000003', 'atc@caac-south.gov.cn', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 4, 1, NOW(), '127.0.0.1'),
(4, 'pilot_zhang', '13800000004', 'pilot_zhang@sf-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 5, 1, NOW(), '127.0.0.1'),
(5, 'maintain_li', '13800000005', 'maintain_li@soarcloud.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 6, 1, NOW(), '127.0.0.1'),
(6, 'customer_demo', '13800000006', 'customer@tencent.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1, 1, NOW(), '127.0.0.1'),
(7, 'merchant_jd_sub', '13800000007', 'dispatch@jd-air.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 1, NOW(), '127.0.0.1'),
(8, 'merchant_pending', '13800000008', 'biz@pending-cargo.com', '/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2, 0, NULL, NULL);

-- 4. 账户-角色多对多关联 (uam_account_role)
TRUNCATE TABLE uam_account_role;
INSERT INTO uam_account_role (ccount_id, 
ole_id) VALUES
(1, 1), -- admin -> ROLE_ADMIN
(2, 2), -- merchant_sf -> ROLE_MERCHANT
(3, 3), -- atc_officer -> ROLE_ATC
(4, 4), -- pilot_zhang -> ROLE_PILOT
(5, 5), -- maintain_li -> ROLE_MAINTAINER
(6, 6), -- customer_demo -> ROLE_CUSTOMER
(7, 2), -- merchant_jd_sub -> ROLE_MERCHANT
(8, 2); -- merchant_pending -> ROLE_MERCHANT

-- 5. 初始化商户档案 (merchant_archive)
TRUNCATE TABLE merchant_archive;
INSERT INTO merchant_archive (merchant_id, ccount_id, corp_name, license_no, contact_name, contact_phone, irworthiness_cert, udit_state, udit_note, udit_at, uditor_id) VALUES
(1, 2, '深圳市丰翼低空物流科技有限公司', '91440300MA5EXXX123', '张经理', '13800000002', 'CAAC-CCAR92-2024-008', 1, '企业适航许可与空运承运资质完备，审核通过', NOW(), 1),
(2, 8, '翼联创新低空货运（深圳）有限公司', '91440300MA5EXXX999', '刘主管', '13800000008', 'CAAC-CCAR92-2024-P01', 0, '新商户入驻申请，待管理员资质审核', NULL, NULL);

-- 6. 初始化客户实名档案 (customer_archive)
TRUNCATE TABLE customer_archive;
INSERT INTO customer_archive (customer_id, ccount_id, 
eal_name, id_card, credit_limit, uth_state) VALUES
(1, 6, '腾讯科技（深圳）低空物资部', '91440300708461136T', 50000.00, 1);

-- 7. 初始化起降场站与智能换电设施 (base_site) - 以深圳核心低空走廊为基准
TRUNCATE TABLE ase_site;
INSERT INTO ase_site (site_id, site_code, site_name, site_geo, capacity, occupied, site_state) VALUES
(1, 'SITE-SZ-001', '南山科技园低空综合起降枢纽', ST_GeomFromText('POINT(113.943 22.540)'), 6, 2, 1),
(2, 'SITE-SZ-002', '福田CBD深业上城楼顶起降场', ST_GeomFromText('POINT(114.062 22.553)'), 4, 1, 1),
(3, 'SITE-SZ-003', '龙岗大运智慧物流换电场站', ST_GeomFromText('POINT(114.218 22.695)'), 8, 3, 1),
(4, 'SITE-SZ-004', '盐田港综合保税区中转场站', ST_GeomFromText('POINT(114.265 22.578)'), 4, 0, 1);

-- 8. 初始化无人机资产台账 (uav_info)
TRUNCATE TABLE uav_info;
INSERT INTO uav_info (uav_id, uav_sn, model, merchant_id, max_load, max_range_km, attery_pct, health_score, ly_hours, state, home_site_id) VALUES
(1, 'UAV-FY90-001', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 92, 98, 142, 0, 1),
(2, 'UAV-FY90-002', '顺丰丰舟-90 (垂起固定翼)', 1, 20.00, 65.0, 85, 95, 110, 1, 1),
(3, 'UAV-FC30-001', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 100, 100, 45, 0, 2),
(4, 'UAV-FC30-002', '大疆运载FC30 (八轴多旋翼)', 1, 30.00, 28.0, 40, 88, 260, 2, 2);

-- 9. 初始化机组人员档案 (crew_staff)
TRUNCATE TABLE crew_staff;
INSERT INTO crew_staff (crew_id, ccount_id, crew_type, 
eal_name, license_no, license_expire, ly_hours, status) VALUES
(1, 4, 1, '张志强', 'CAAC-UAV-20210988', '2028-12-31', 480, 1),
(2, 5, 2, '李维保', 'MAINT-CERT-2022011', '2027-10-30', 0, 1);

-- 10. 初始化空域管制区块网格 (airspace_block)
TRUNCATE TABLE irspace_block;
-- 禁飞区1：深圳宝安机场净空保护禁飞区
INSERT INTO irspace_block (lock_id, lock_name, lock_kind, geo_polygon, limit_alt, source) VALUES
(1, '深圳宝安国际机场核心净空禁飞区', 1,
 ST_GeomFromText('POLYGON((113.78 22.60, 113.84 22.60, 113.84 22.66, 113.78 22.66, 113.78 22.60))'), 0, '民航中南局');

-- 限飞区2：南山高新科技园核心限高管控区（限高120米）
INSERT INTO irspace_block (lock_id, lock_name, lock_kind, geo_polygon, limit_alt, source) VALUES
(2, '南山科技园总部基地限高管制区', 2,
 ST_GeomFromText('POLYGON((113.93 22.52, 113.96 22.52, 113.96 22.56, 113.93 22.56, 113.93 22.52))'), 120, '市低空空管委');

-- 临时管制区3：大运中心近期重要活动临时管制（限高80米）
INSERT INTO irspace_block (lock_id, lock_name, lock_kind, geo_polygon, limit_alt, effective_at, expire_at, source) VALUES
(3, '大运体育中心赛事临时管制空域', 3,
 ST_GeomFromText('POLYGON((114.20 22.68, 114.23 22.68, 114.23 22.71, 114.20 22.71, 114.20 22.68))'), 80,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), '公安低空警卫办');

-- 11. 初始化电子围栏规则 (fence_rule)
TRUNCATE TABLE ence_rule;
INSERT INTO ence_rule (
ule_id, 
ule_name, uffer_distance, max_altitude, ction_policy) VALUES
(1, '重点敏感目标法定禁飞规则', 50, 0, 3),
(2, '城市建成区法定真高上限规则', 30, 120, 2),
(3, '沿海航道动态预警缓冲规则', 100, 150, 1);

-- 12. 初始化系统运行参数 (sys_param)
TRUNCATE TABLE sys_param;
INSERT INTO sys_param (param_id, param_group, param_key, param_value, param_desc) VALUES
(1, 'fee_rule', 'BASE_FREIGHT_PRICE', '35.00', '低空货运首重起步运费（元/首重3kg）'),
(2, 'fee_rule', 'PER_KM_SURCHARGE', '4.50', '每千米航程附加费（元/km）'),
(3, 'fee_rule', 'AIRSPACE_PASS_PRICE', '12.00', '每单法定低空空域通道使用费（元）'),
(4, 'alarm_threshold', 'ROUTE_DEVIATION_METERS', '50', '飞行航线偏航触发告警最大允许偏离距离（米）'),
(5, 'alarm_threshold', 'LOW_BATTERY_PERCENT', '20', '在途低电量自动返航告警百分比阈值'),
(6, 'order_policy', 'ORDER_AUTO_CANCEL_MINUTES', '15', '下单未支付自动超时关闭时间（分钟）');

-- 13. 初始化外部接口网关健康登记 (api_endpoint)
TRUNCATE TABLE pi_endpoint;
INSERT INTO pi_endpoint (endpoint_id, service_name, endpoint_url, method, status_code, latency_ms, success_rate, check_time) VALUES
(1, '民航低空协同审批系统网关 (CAAC)', 'https://atc.soarcloud.com/gateway/flight/verify', 'POST', 200, 42, 99.98, NOW()),
(2, '高德地图Web API逆地理编码服务', 'https://restapi.amap.com/v3/geocode/regeo', 'GET', 200, 18, 100.00, NOW()),
(3, '中央气象台低空微气象与阵风预警', 'https://weather.soarcloud.com/api/v2/aviation-wind', 'GET', 200, 31, 99.85, NOW()),
(4, '中国银联 / 微信企业对账支付网关', 'https://api.mch.weixin.qq.com/v3/pay/transactions', 'POST', 200, 56, 100.00, NOW());

SET FOREIGN_KEY_CHECKS = 1;
