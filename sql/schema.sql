-- ====================================================================
-- 翔云·低空智慧物流运营平台 (fly_manager_platform / soarcloud_lae)
-- 数据库物理设计脚本 (schema.sql)
-- 包含 7 大业务域 27 张完整业务单据表，具备 InnoDB 事务与 MySQL 空间扩展
-- 字符集：utf8mb4，排序规则：utf8mb4_general_ci
-- ====================================================================

CREATE DATABASE IF NOT EXISTS soarcloud_lae DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE soarcloud_lae;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 统一账户主表 (uam_account)
DROP TABLE IF EXISTS uam_account;
CREATE TABLE uam_account (
  account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账户唯一标识（主键）',
  login_name VARCHAR(32) NOT NULL COMMENT '登录用户名（唯一）',
  mobile CHAR(11) DEFAULT NULL COMMENT '绑定手机号（唯一）',
  email VARCHAR(64) DEFAULT NULL COMMENT '绑定电子邮箱',
  pwd_digest VARCHAR(100) NOT NULL COMMENT 'Argon2id/BCrypt 加盐密码散列哈希',
  account_kind TINYINT NOT NULL COMMENT '账户类型：1-普通客户 2-物流商户 3-平台管理员 4-空管监察员 5-无人机机长 6-运维专员',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态：0-待审核 1-正常 2-冻结 3-注销',
  last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
  last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP地址',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (account_id),
  UNIQUE KEY uk_account_login_name (login_name),
  UNIQUE KEY uk_account_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一账户主表（多角色合一底座）';

-- 2. RBAC角色定义表 (uam_role)
DROP TABLE IF EXISTS uam_role;
CREATE TABLE uam_role (
  
role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色唯一标识（主键）',
  
role_code VARCHAR(32) NOT NULL COMMENT '角色唯一编码',
  
role_name VARCHAR(64) NOT NULL COMMENT '角色中文名称',
  description VARCHAR(255) DEFAULT NULL COMMENT '角色职责描述',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (role_id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RBAC基础角色定义表';

-- 3. 账户-角色多对多关联表 (uam_account_role)
DROP TABLE IF EXISTS uam_account_role;
CREATE TABLE uam_account_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关联主键ID',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '关联账户ID',
  
role_id BIGINT UNSIGNED NOT NULL COMMENT '关联角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_acc_role (account_id, 
role_id),
  KEY idx_role_id (
role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账户与角色多对多关联表';

-- 4. 角色-权限(菜单/按钮)授权表 (uam_role_grant)
DROP TABLE IF EXISTS uam_role_grant;
CREATE TABLE uam_role_grant (
  grant_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '授权主键ID',
  
role_id BIGINT UNSIGNED NOT NULL COMMENT '关联角色ID',
  perm_code VARCHAR(64) NOT NULL COMMENT '权限唯一编码',
  perm_name VARCHAR(64) DEFAULT NULL COMMENT '权限说明名称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (grant_id),
  KEY idx_role_grant (
role_id, perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色与权限资源关联表';

-- 5. 商户企业资质档案表 (merchant_archive)
DROP TABLE IF EXISTS merchant_archive;
CREATE TABLE merchant_archive (
  merchant_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商户企业唯一标识',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '关联主账号ID',
  corp_name VARCHAR(128) NOT NULL COMMENT '企业全称',
  license_no VARCHAR(32) NOT NULL COMMENT '统一社会信用代码/营业执照号',
  contact_name VARCHAR(32) NOT NULL COMMENT '企业联系人真实姓名',
  contact_phone CHAR(11) NOT NULL COMMENT '企业联系人手机号',
  airworthiness_cert VARCHAR(64) DEFAULT NULL COMMENT '通用航空经营许可证/适航许可文号',
  audit_state TINYINT NOT NULL DEFAULT 0 COMMENT '入驻审批状态：0-待审核 1-已通过 2-已驳回',
  audit_note VARCHAR(255) DEFAULT NULL COMMENT '审批说明或驳回理由',
  audit_at DATETIME DEFAULT NULL COMMENT '资质审核时间',
  auditor_id BIGINT UNSIGNED DEFAULT NULL COMMENT '执行审核管理员账户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (merchant_id),
  UNIQUE KEY uk_merchant_license (license_no),
  KEY idx_merchant_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商户企业资质档案表';

-- 6. 客户实名档案表 (customer_archive)
DROP TABLE IF EXISTS customer_archive;
CREATE TABLE customer_archive (
  customer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '客户档案唯一标识',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '关联账户ID',
  
real_name VARCHAR(32) NOT NULL COMMENT '客户真实姓名/企业名称',
  id_card VARCHAR(18) NOT NULL COMMENT '身份证号码/统一信用代码',
  credit_limit DECIMAL(10,2) NOT NULL DEFAULT 5000.00 COMMENT '月结月度信用额度（元）',
  auth_state TINYINT NOT NULL DEFAULT 1 COMMENT '实名认证状态：0-未认证 1-已认证 2-认证失败',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (customer_id),
  KEY idx_customer_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户实名档案表';

-- 7. 资质证照文件附件表 (cert_file)
DROP TABLE IF EXISTS cert_file;
CREATE TABLE cert_file (
  cert_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '证照附件唯一标识',
  owner_type TINYINT NOT NULL COMMENT '所属主体类型：1-商户 2-无人机 3-机组执照',
  owner_id BIGINT UNSIGNED NOT NULL COMMENT '关联主体ID',
  cert_type VARCHAR(32) NOT NULL COMMENT '证件类型（如 LICENSE, AIR_CERT, PILOT_CERT）',
  file_path VARCHAR(255) NOT NULL COMMENT '存储相对路径或访问URL',
  file_hash CHAR(64) DEFAULT NULL COMMENT '文件SHA256哈希指纹',
  expire_at DATETIME DEFAULT NULL COMMENT '证照有效期截止日期',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (cert_id),
  KEY idx_cert_owner (owner_type, owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资质证照文件附件存储表';

-- 8. 起降场站与换电设施表 (base_site)
DROP TABLE IF EXISTS base_site;
CREATE TABLE base_site (
  site_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '场站唯一标识',
  site_code VARCHAR(32) NOT NULL COMMENT '场站编码（唯一）',
  site_name VARCHAR(64) NOT NULL COMMENT '场站中文名称',
  site_geo POINT NOT NULL COMMENT '场站中心经纬度坐标（空间点）',
  capacity INT NOT NULL DEFAULT 4 COMMENT '停机位与换电机柜总容量',
  occupied INT NOT NULL DEFAULT 0 COMMENT '当前已占用机位数',
  site_state TINYINT NOT NULL DEFAULT 1 COMMENT '场站运行状态：1-正常运营 2-临时停运 3-设备维护',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (site_id),
  UNIQUE KEY uk_site_code (site_code),
  SPATIAL KEY idx_site_geo (site_geo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='起降场站与智能换电设施台账表';

-- 9. 无人机资产台账表 (uav_info)
DROP TABLE IF EXISTS uav_info;
CREATE TABLE uav_info (
  uav_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '无人机资产唯一标识',
  uav_sn VARCHAR(32) NOT NULL COMMENT '机身设备唯一编号',
  model VARCHAR(64) NOT NULL COMMENT '无人机机型',
  merchant_id BIGINT UNSIGNED NOT NULL COMMENT '所属物流商户企业ID',
  max_load DECIMAL(6,2) NOT NULL COMMENT '最大额定载重（千克）',
  max_range_km DECIMAL(6,1) NOT NULL COMMENT '最大标称航程（千米）',
  battery_pct TINYINT NOT NULL DEFAULT 100 COMMENT '当前电池剩余电量百分比（0-100）',
  health_score TINYINT NOT NULL DEFAULT 100 COMMENT '设备健康综合评分（0-100）',
  fly_hours INT NOT NULL DEFAULT 0 COMMENT '累计飞行总时长（小时）',
  
next_maintain_at DATETIME DEFAULT NULL COMMENT '下次计划维保日期',
  state TINYINT NOT NULL DEFAULT 0 COMMENT '设备五态：0-闲置 1-任务中 2-维保中 3-故障 4-离线',
  home_site_id BIGINT UNSIGNED DEFAULT NULL COMMENT '常驻基地场站ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (uav_id),
  UNIQUE KEY uk_uav_sn (uav_sn),
  KEY idx_uav_merchant (merchant_id),
  KEY idx_uav_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='无人机资产台账表';

-- 10. 无人机维保与故障记录表 (uav_maintain)
DROP TABLE IF EXISTS uav_maintain;
CREATE TABLE uav_maintain (
  maintain_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '维保记录唯一标识',
  uav_id BIGINT UNSIGNED NOT NULL COMMENT '关联无人机ID',
  maintain_type TINYINT NOT NULL COMMENT '类型：1-定期巡检保养 2-故障上报维修',
  fault_desc VARCHAR(255) DEFAULT NULL COMMENT '故障现象描述',
  
repair_content VARCHAR(255) DEFAULT NULL COMMENT '维保处理与更换配件记录',
  operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT '执行维保人员账户ID',
  maintain_state TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待维保 1-维保中 2-已验收合格',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (maintain_id),
  KEY idx_maintain_uav (uav_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='无人机维保工单与故障排查记录表';

-- 11. 场站机位与起降预约表 (site_booking)
DROP TABLE IF EXISTS site_booking;
CREATE TABLE site_booking (
  booking_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约唯一标识',
  site_id BIGINT UNSIGNED NOT NULL COMMENT '目标场站ID',
  uav_id BIGINT UNSIGNED NOT NULL COMMENT '预约无人机ID',
  apply_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联飞行申报单ID',
  start_time DATETIME NOT NULL COMMENT '预约占用起始时间',
  end_time DATETIME NOT NULL COMMENT '预约占用截止时间',
  booking_state TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-已预约 2-已占用 3-已释放 4-已取消',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (booking_id),
  KEY idx_booking_site_time (site_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='场站机位与起降预约时空防冲表';

-- 12. 机组执勤人员档案表 (crew_staff)
DROP TABLE IF EXISTS crew_staff;
CREATE TABLE crew_staff (
  crew_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '机组人员档案唯一标识',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '关联系统账户ID',
  crew_type TINYINT NOT NULL COMMENT '岗位类别：1-商用机长(飞手) 2-外场运维专员',
  
real_name VARCHAR(32) NOT NULL COMMENT '真实姓名',
  license_no VARCHAR(32) NOT NULL COMMENT '民用无人机驾驶员执照编号',
  license_expire DATE NOT NULL COMMENT '执照有效期截止日期',
  fly_hours INT NOT NULL DEFAULT 0 COMMENT '累计安全执飞时长（小时）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '执勤状态：1-在岗 2-轮休 3-离职',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (crew_id),
  UNIQUE KEY uk_crew_license (license_no),
  KEY idx_crew_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机组执勤人员资质台账表';

-- 13. 机组排班计划表 (crew_shift)
DROP TABLE IF EXISTS crew_shift;
CREATE TABLE crew_shift (
  shift_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排班唯一标识',
  crew_id BIGINT UNSIGNED NOT NULL COMMENT '机组人员ID',
  shift_date DATE NOT NULL COMMENT '排班执勤日期',
  start_time TIME NOT NULL COMMENT '值班起始时刻',
  end_time TIME NOT NULL COMMENT '值班截止时刻',
  shift_state TINYINT NOT NULL DEFAULT 1 COMMENT '排班状态：1-已排班 2-执勤中 3-已完成 4-请假调休',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (shift_id),
  KEY idx_crew_shift_date (crew_id, shift_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机组排班计划矩阵表';

-- 14. 货运订单主表 (cargo_order)
DROP TABLE IF EXISTS cargo_order;
CREATE TABLE cargo_order (
  order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单唯一标识（主键）',
  order_sn VARCHAR(24) NOT NULL COMMENT '业务订单流水号',
  customer_id BIGINT UNSIGNED NOT NULL COMMENT '下单客户档案ID',
  merchant_id BIGINT UNSIGNED DEFAULT NULL COMMENT '承接履约商户企业ID',
  sender_addr VARCHAR(255) NOT NULL COMMENT '寄件地址文本',
  
receiver_addr VARCHAR(255) NOT NULL COMMENT '收件地址文本',
  sender_geo POINT NOT NULL COMMENT '寄件地理经纬度坐标',
  
receiver_geo POINT NOT NULL COMMENT '收件地理经纬度坐标',
  goods_type TINYINT NOT NULL COMMENT '货物类别：1-普通文件 2-生物医疗急救 3-数码生鲜 4-高附加值工业件',
  goods_weight DECIMAL(8,2) NOT NULL COMMENT '货物重量（千克）',
  urgent_flag TINYINT NOT NULL DEFAULT 0 COMMENT '加急标记：0-标准时效 1-加急优先配送',
  fee_base DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '基础运费（元）',
  fee_airspace DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '空域通道占用费（元）',
  fee_urgent DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加急调度附加费（元）',
  fee_discount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '满减优惠券抵扣金额（元）',
  fee_total DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '应付总金额（元）',
  pay_way TINYINT NOT NULL DEFAULT 1 COMMENT '支付途径：1-在线微信/支付宝 2-企业月结扣款',
  pay_state TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态：0-未支付 1-已支付 2-已退款',
  order_state TINYINT NOT NULL DEFAULT 10 COMMENT '订单九态：10-待支付 20-待接单 30-待调度 40-待起飞 50-飞行中 60-已送达 70-已取消 80-异常中断',
  uav_id BIGINT UNSIGNED DEFAULT NULL COMMENT '执飞无人机资产ID',
  apply_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联飞行申报单ID',
  expect_arrive_at DATETIME DEFAULT NULL COMMENT '期望最晚送达时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_order_sn (order_sn),
  KEY idx_order_customer (customer_id, created_at),
  KEY idx_order_merchant (merchant_id, order_state, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低空货运订单核心业务主表';

-- 15. 订单状态变迁流水表 (order_state_flow)
DROP TABLE IF EXISTS order_state_flow;
CREATE TABLE order_state_flow (
  flow_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '状态流水ID',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '所属业务订单ID',
  from_state TINYINT NOT NULL COMMENT '变迁前状态',
    to_state TINYINT NOT NULL COMMENT '变迁后目标状态',
  operator_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '触发操作人ID',
  
remark VARCHAR(255) DEFAULT NULL COMMENT '流转说明或异常原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变迁发生时间',
  PRIMARY KEY (flow_id),
  KEY idx_flow_order (order_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单全生命周期状态流转流水表';

-- 16. 货运结算对账单表 (settle_bill)
DROP TABLE IF EXISTS settle_bill;
CREATE TABLE settle_bill (
  bill_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账单唯一标识',
  bill_sn VARCHAR(24) NOT NULL COMMENT '对账单编号',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '关联订单ID',
  merchant_id BIGINT UNSIGNED NOT NULL COMMENT '结算商户ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '结算应付总额（元）',
  bill_state TINYINT NOT NULL DEFAULT 0 COMMENT '账单状态：0-待出账 1-待对账 2-已对账 3-已开票',
  invoice_flag TINYINT NOT NULL DEFAULT 0 COMMENT '发票开具标记：0-未开票 1-已开票',
  settle_at DATETIME DEFAULT NULL COMMENT '系统出账时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (bill_id),
  UNIQUE KEY uk_bill_sn (bill_sn),
  UNIQUE KEY uk_bill_order (order_id),
  KEY idx_bill_merchant (merchant_id, bill_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='货运结算与财务对账单据表';

-- 17. 空域管制区块网格表 (aairspace_block)
DROP TABLE IF EXISTS airspace_block;
CREATE TABLE airspace_block (
  block_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '空域区块唯一标识',
  block_name VARCHAR(64) NOT NULL COMMENT '空域区块中文名称',
  block_kind TINYINT NOT NULL COMMENT '管控类别：1-永久禁飞区 2-动态限飞区 3-临时管制区',
  geo_polygon POLYGON NOT NULL COMMENT '区块多边形几何空间范围',
  limit_alt INT DEFAULT NULL COMMENT '限飞高度上限（米）',
  effective_at DATETIME DEFAULT NULL COMMENT '管制生效起始时间',
  expire_at DATETIME DEFAULT NULL COMMENT '管制失效截止时间',
  source VARCHAR(32) NOT NULL DEFAULT '空管同步' COMMENT '数据来源渠道',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (block_id),
  SPATIAL KEY idx_block_geo (geo_polygon)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低空空域管控区块地理网格表';

-- 18. 飞行计划申报主表 (flight_apply)
DROP TABLE IF EXISTS flight_apply;
CREATE TABLE flight_apply (
  apply_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申报唯一标识',
  apply_sn VARCHAR(24) NOT NULL COMMENT '申报编号',
  order_id BIGINT UNSIGNED DEFAULT 0 COMMENT '关联履约订单ID（0代表独立自主申报）',
  merchant_id BIGINT UNSIGNED NOT NULL COMMENT '申报商户企业ID',
  uav_id BIGINT UNSIGNED NOT NULL COMMENT '执飞无人机资产ID',
  pilot_id BIGINT UNSIGNED NOT NULL COMMENT '执飞机长责任人ID',
  
route_line LINESTRING NOT NULL COMMENT '规划飞行航线（空间折线）',
  alt_planned INT NOT NULL COMMENT '申报飞行巡航高度（米）',
  start_at DATETIME NOT NULL COMMENT '计划起飞时间',
  end_at DATETIME NOT NULL COMMENT '计划降落时间',
  check_state TINYINT NOT NULL DEFAULT 0 COMMENT '系统围栏预校验：0-未校验 1-校验通过 2-冲突告警',
  approve_state TINYINT NOT NULL DEFAULT 0 COMMENT '申报审批四态：0-待审批 1-已通过 2-已驳回 3-已撤销',
  approver_id BIGINT UNSIGNED DEFAULT NULL COMMENT '审批空管人员ID',
  approve_note VARCHAR(255) DEFAULT NULL COMMENT '审批批复意见或限高建议',
  approve_at DATETIME DEFAULT NULL COMMENT '审批决策时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (apply_id),
  UNIQUE KEY uk_aapply_sn (apply_sn),
  KEY idx_apply_order (order_id),
  UNIQUE KEY uk_flight_apply_order ((IF(order_id > 0, order_id, NULL))),
  KEY idx_apply_state (approve_state),
  SPATIAL KEY idx_apply_route (
route_line)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低空飞行计划申报与合规审批表';

-- 19. 电子围栏合规校验规则表 (fence_rule)
DROP TABLE IF EXISTS fence_rule;
CREATE TABLE fence_rule (
  
rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则唯一标识',
  
rule_name VARCHAR(64) NOT NULL COMMENT '规则名称',
  buffer_distance INT NOT NULL DEFAULT 50 COMMENT '外围预警安全缓冲带距离（米）',
  max_altitude INT NOT NULL DEFAULT 120 COMMENT '低空默认法定飞行真高上限（米）',
  action_policy TINYINT NOT NULL DEFAULT 1 COMMENT '碰撞触发动作：1-弹窗告警 2-阻断申报 3-强制降落',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (
rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='电子围栏合规校验规则字典表';

-- 20. 飞行违规案件记录表 (violation_case)
DROP TABLE IF EXISTS violation_case;
CREATE TABLE violation_case (
  case_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '违规案件唯一标识',
  case_sn VARCHAR(24) NOT NULL COMMENT '案件编号',
  apply_id BIGINT UNSIGNED NOT NULL COMMENT '关联飞行计划申报单ID',
  violate_kind TINYINT NOT NULL COMMENT '违规行为类别：1-航线严重偏航 2-非法入侵禁飞区 3-超越法定限高 4-超时占用空域',
  level TINYINT NOT NULL DEFAULT 1 COMMENT '行政处罚分级：1-警告留痕 2-企业罚款 3-停飞整顿 4-吊销资质',
  punish_state TINYINT NOT NULL DEFAULT 0 COMMENT '案件处置状态：0-待核查 1-核查中 2-已结案下达文书',
  evidence VARCHAR(255) DEFAULT NULL COMMENT '存证佐证数据',
  handled_by BIGINT UNSIGNED DEFAULT NULL COMMENT '处置监察员ID',
  handled_at DATETIME DEFAULT NULL COMMENT '案件办结时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (case_id),
  UNIQUE KEY uk_case_sn (case_sn),
  KEY idx_case_apply (apply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='低空飞行违规调查与行政执法留痕表';

-- 21. 实时告警事件记录表 (aalarm_event)
DROP TABLE IF EXISTS alarm_event;
CREATE TABLE alarm_event (
  alarm_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '告警唯一标识',
  alarm_sn VARCHAR(24) NOT NULL COMMENT '告警流水号',
  apply_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联飞行申报ID',
  uav_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联无人机资产ID',
  alarm_level TINYINT NOT NULL COMMENT '告警级别：1-一般 2-严重 3-紧急',
  alarm_type VARCHAR(32) NOT NULL COMMENT '告警类型',
  content VARCHAR(255) NOT NULL COMMENT '告警异常详情描述',
  handle_state TINYINT NOT NULL DEFAULT 0 COMMENT '处置状态：0-待处置 1-处置中 2-已解除恢复',
  handle_note TEXT DEFAULT NULL COMMENT '应急处置措施与全流程时间线记录',
  handled_at DATETIME DEFAULT NULL COMMENT '告警解除时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (alarm_id),
  UNIQUE KEY uk_aalarm_sn (alarm_sn),
  KEY idx_alarm_apply (apply_id),
  KEY idx_aalarm_level (alarm_level, handle_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='在途应急监控与三级告警事件记录表';

-- 22. 客户服务与异常工单表 (service_ticket)
DROP TABLE IF EXISTS service_ticket;
CREATE TABLE service_ticket (
  ticket_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单唯一标识',
  ticket_sn VARCHAR(24) NOT NULL COMMENT '工单业务流水号',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '提交客户/机长账户ID',
  order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联涉及订单ID',
  ticket_type TINYINT NOT NULL COMMENT '工单类别：1-业务咨询 2-配送时效投诉 3-设备故障报修 4-费用对账异议',
    title VARCHAR(128) NOT NULL COMMENT '工单简述标题',
  content TEXT NOT NULL COMMENT '详细工单述求内容',
    ticket_state TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-待受理 1-处理中 2-已答复办结',
  
reply_content TEXT DEFAULT NULL COMMENT '官方答复内容',
  handled_by BIGINT UNSIGNED DEFAULT NULL COMMENT '办结客服专员ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (ticket_id),
  UNIQUE KEY uk_ticket_sn (ticket_sn),
  KEY idx_ticket_account (account_id),
  KEY idx_ticket_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户服务与运维协同工单表';

-- 23. 平台站内消息与触达通知表 (msg_notice)
DROP TABLE IF EXISTS msg_notice;
CREATE TABLE msg_notice (
  msg_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息通知主键ID',
  
receiver_id BIGINT UNSIGNED NOT NULL COMMENT '接收方账户ID',
    title VARCHAR(128) NOT NULL COMMENT '消息通知标题',
  content VARCHAR(512) NOT NULL COMMENT '消息主体文本内容',
  msg_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-系统公告 2-业务审批 3-在途告警 4-订单提醒',
  is_read TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0-未读 1-已读',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (msg_id),
  KEY idx_msg_receiver (
receiver_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台站内消息与多端推送通知表';

-- 24. 飞行轨迹明细数据表 (fly_trace)
DROP TABLE IF EXISTS fly_trace;
CREATE TABLE fly_trace (
    trace_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '航迹点记录唯一标识',
  apply_id BIGINT UNSIGNED NOT NULL COMMENT '关联飞行申报单ID',
  uav_id BIGINT UNSIGNED NOT NULL COMMENT '执飞无人机资产ID',
  point_geo POINT NOT NULL COMMENT '瞬时上报空间经纬度坐标',
  altitude DECIMAL(6,2) NOT NULL COMMENT '实测飞行真高海拔（米）',
  speed DECIMAL(5,2) NOT NULL COMMENT '瞬时水平飞行地速（米/秒）',
  battery_remain TINYINT NOT NULL COMMENT '实时剩余电量百分比',
  
report_time DATETIME NOT NULL COMMENT '设备硬件采样时间戳',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库持久化时间',
  PRIMARY KEY (  trace_id),
  KEY idx_trace_apply_time (apply_id, 
report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='无人机在途高频飞行航迹数据表';

-- 25. 平台基础运行参数配置表 (sys_param)
DROP TABLE IF EXISTS sys_param;
CREATE TABLE sys_param (
  param_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参数唯一标识',
  param_group VARCHAR(32) NOT NULL COMMENT '参数分组',
  param_key VARCHAR(64) NOT NULL COMMENT '参数Key（唯一）',
  param_value VARCHAR(255) NOT NULL COMMENT '参数实际生效值',
  param_desc VARCHAR(128) DEFAULT NULL COMMENT '业务参数中文说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (param_id),
  UNIQUE KEY uk_param_key (param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='平台业务规则与系统运行参数配置表';

-- 26. 外部三方服务网关监控表 (api_endpoint)
DROP TABLE IF EXISTS api_endpoint;
CREATE TABLE api_endpoint (
  endpoint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '接口网关监控ID',
  service_name VARCHAR(64) NOT NULL COMMENT '三方服务名称',
  endpoint_url VARCHAR(255) NOT NULL COMMENT '接口URL基地址',
  method VARCHAR(10) NOT NULL DEFAULT 'GET' COMMENT '探活请求方式',
  status_code INT NOT NULL DEFAULT 200 COMMENT '最后一次探测HTTP响应码',
  latency_ms INT NOT NULL DEFAULT 0 COMMENT '心跳往返耗时（毫秒）',
  success_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '24小时探测可用率百分比',
  check_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后探活探测时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0-正常 1-已删除',
  PRIMARY KEY (endpoint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='外部三方服务依赖网关探活与连通性监控表';

-- 27. 敏感操作全局审计日志表 (audit_log)
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
  log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计日志唯一ID',
  account_id BIGINT UNSIGNED NOT NULL COMMENT '执行操作人账户ID',
  login_name VARCHAR(32) NOT NULL COMMENT '操作人登录账号名称',
  module_name VARCHAR(64) NOT NULL COMMENT '所属业务功能模块',
  action_name VARCHAR(64) NOT NULL COMMENT '具体操作行为',
  
request_uri VARCHAR(255) NOT NULL COMMENT '请求HTTP接口路由',
  client_ip VARCHAR(45) NOT NULL COMMENT '客户端发起真实IP',
  client_kind VARCHAR(10) NOT NULL DEFAULT 'pc' COMMENT '发起终端：pc 或 app',
  cost_time BIGINT NOT NULL COMMENT '服务端处理执行耗时（毫秒）',
  
req_params TEXT DEFAULT NULL COMMENT '请求入参Payload（脱敏后）',
  
resp_status INT NOT NULL DEFAULT 200 COMMENT '业务执行响应状态码',
  error_msg TEXT DEFAULT NULL COMMENT '异常失败捕获堆栈或提示',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作行为发生时间',
  PRIMARY KEY (log_id),
  KEY idx_audit_account (account_id, created_at),
  KEY idx_audit_module (module_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='敏感业务与核心操作全局审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
