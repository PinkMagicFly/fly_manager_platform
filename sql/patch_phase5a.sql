-- ====================================================================
-- 阶段5A DDL 变更补丁脚本：飞行任务时序重排与 order_id 唯一约束
-- 涉及表：flight_apply
-- 规则基线：开发计划 §2.1 双单据模型（关系 1:1，order_id>0 唯一约束，一单一任务）
-- ====================================================================

-- 1. 为 flight_apply 表增加 order_id > 0 条件下的唯一约束（自主飞行 order_id=0 允许重复）
ALTER TABLE flight_apply ADD UNIQUE KEY uk_flight_apply_order ((IF(order_id > 0, order_id, NULL)));

-- 2. 字段中文注释更新（任务状态机 4 -> 6 态）
ALTER TABLE flight_apply MODIFY COLUMN approve_state TINYINT NOT NULL DEFAULT 0 
COMMENT '申报审批六态：0-待审批 1-已批准 2-已驳回 3-已取消 4-执行中 5-已完成';
