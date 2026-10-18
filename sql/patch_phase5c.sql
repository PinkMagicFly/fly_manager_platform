-- ====================================================================
-- 翔云·低空智慧物流运营平台 - 阶段 5C 增量数据库修复补丁脚本
-- 1. 扩大 alarm_event 表 handle_note 列为 TEXT，支持完整存储 JSON 时间线流水
-- ====================================================================

USE soarcloud_lae;

ALTER TABLE alarm_event MODIFY COLUMN handle_note TEXT DEFAULT NULL COMMENT '应急处置措施与全流程时间线记录';

-- 2. 客户下单即支付闭环，取消无法测试的待支付超时取消参数设置
UPDATE sys_param SET is_deleted = 1 WHERE param_key IN ('order.timeout_pay_minutes', 'ORDER_AUTO_CANCEL_MINUTES');

