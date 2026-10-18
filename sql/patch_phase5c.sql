-- ====================================================================
-- 翔云·低空智慧物流运营平台 - 阶段 5C 增量数据库修复补丁脚本
-- 1. 扩大 alarm_event 表 handle_note 列为 TEXT，支持完整存储 JSON 时间线流水
-- ====================================================================

USE soarcloud_lae;

ALTER TABLE alarm_event MODIFY COLUMN handle_note TEXT DEFAULT NULL COMMENT '应急处置措施与全流程时间线记录';

-- 2. 客户下单即支付闭环，取消无法测试的待支付超时取消参数设置
UPDATE sys_param SET is_deleted = 1 WHERE param_key IN ('order.timeout_pay_minutes', 'ORDER_AUTO_CANCEL_MINUTES');

-- 3. 规范系统运行参数中文描述（防止客户端编码不一致导致展示异常）
SET NAMES utf8mb4;
UPDATE sys_param SET param_desc = '低空货运首重起步运费（元）' WHERE param_key = 'fee.base_start';
UPDATE sys_param SET param_desc = '超航程每公里附加单价（元/km）' WHERE param_key = 'fee.per_km';
UPDATE sys_param SET param_desc = '加急配送费率倍率系数' WHERE param_key = 'fee.urgent_ratio';
UPDATE sys_param SET param_desc = '商户待接单超时自动取消时间（分钟）' WHERE param_key = 'order.timeout_accept_minutes';
UPDATE sys_param SET param_desc = '告警超时未处置预警时间（分钟）' WHERE param_key = 'alarm.timeout_minutes';
UPDATE sys_param SET param_desc = '在途飞行偏航告警阈值（米）' WHERE param_key = 'alarm.route_deviation_meters';
UPDATE sys_param SET param_desc = '低空货运首重起步运费（元/首重3kg）' WHERE param_key = 'BASE_FREIGHT_PRICE';
UPDATE sys_param SET param_desc = '每千米航程附加费（元/km）' WHERE param_key = 'PER_KM_SURCHARGE';
UPDATE sys_param SET param_desc = '每单法定低空空域通道使用费（元）' WHERE param_key = 'AIRSPACE_PASS_PRICE';


