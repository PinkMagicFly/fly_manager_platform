# 夜间挂机开发进度指针（外层脚本与代理共用，每阶段必须更新）

STATUS: running
PHASE: 5B

> **使用规则**：本文件是挂机循环的唯一断点记忆。每完成一个阶段：
> ① 把 `PHASE` 改为下一阶段；② 文末"阶段日志"追加记录；③ commit 并推送双远端。
> 全部自动阶段（5A~6）完成 → `STATUS: done`；无法继续 → `STATUS: blocked` 并写明原因。
> 到达阶段 7 → 保持 `STATUS: running`、`PHASE: 7`，外层脚本自动停止（人工段）。
> **顶部两行只改值不改格式。** 阶段任务详情与 DoD 见 `docs/开发计划.md` §五。

---

## 阶段索引（详情以开发计划为准）

| PHASE | 名称 | 开发计划章节 | 核心项 |
|-------|------|--------------|--------|
| 5A | 订单-任务时序重排 | §5.1 | G1 提交任务不锁资源 / G2 任务六态 / G3 驳回原因必填 / G4 起飞守卫 / G6 order_id 唯一约束 |
| 5B | 在途监控环 | §5.2 | G5 轨迹上报+回放比对 / G11 map-data 聚合 + P-16 实时图层 |
| 5C | 告警工单与参数恢复环 | §5.3 | G7 告警环(联动订单80) / G8 工单环 / G9 sys_param 参数化计费+订单超时取消 |
| 5D | 补缺与规范收敛 | §5.4 | G10 地址簿(customer_address) / G12 空管账号后台创建+首登改密 / G13 状态码禁上屏 |
| 6 | 统合联调·多维测试·压测·安全 | §5.5 | E2E十步链(含驳回/异常分支) / Client-Kind断言 / 覆盖率≥85% / JMeter / 安全用例 |
| 7 | 人工段（挂机不执行） | §5.6 | 容器化 / APK / 结项报告 / 全量人工验收 |

---

## 阶段日志（每阶段完成后追加：做了什么 / 测试结果 / commit 哈希）

### 阶段 5A：订单-任务时序重排（G1/G2/G3/G4/G6）
- **做了什么**：
  1. **G2 任务状态机 4→6 态**：更新 `FlightApplyStateEnum` 为 `0待审批/1已批准/2已驳回/3已取消/4执行中/5已完成`；升级 `FlightApplyStateMachine`，补充起飞、送达守卫与强制终止流转方法；
  2. **G1 调度确认语义重排**：`dispatchOrder` 重塑为"提交飞行任务"，生成任务 0待审批，不锁无人机/场站资源，订单停留 30 待调度；空管 `approveApply` 审批通过时回调执行：空域时段确权 + 无人机排他性时段冲突检测 + 机长校验 + 场站起降机位预约（`site_booking`）+ 推进订单至 40 待起飞并记录 `order_state_flow` 流水；
  3. **G3 驳回原因必填**：`FlightApproveDTO` 增加 `@NotBlank` 注解，`FlightApplyServiceImpl` 与状态机增加双重非空校验阻断；
  4. **G4 起飞守卫**：`takeoffOrder` 增加关联任务状态前置守卫（必须为 1已批准），起飞联动任务→4执行中且无人机→1任务中；送达联动任务→5已完成且无人机→0闲置；
  5. **G6 order_id>0 唯一约束与防重**：创建 `sql/patch_phase5a.sql`，更新 `sql/schema.sql` 与 `schema-h2.sql`；Service 增加非终态任务防重复提交拦截与驳回重报更新复用支持；
  6. **前端重塑**：P-10 调度台按钮调整为"提交飞行任务审批"并修正弹窗说明；P-18 审批台通过提示锁定资源并推进订单、驳回弹窗原因必填并支持六态筛选标签；订单看板与移动端状态标签中文化去数字码；
  7. **测试覆盖**：重塑 `FlightApplyStateMachineTest`（9/9全过）与 `OrderFeatureChainTest`（新时序闭环通过）；新增 `FlightApplyLockChainTest`（提交零占用→审批锁定→时段冲突拦截→驳回重报→起飞守卫→重复提交拦截端到端全绿）。
- **测试结果**：
  - 后端：`cd backend && mvn clean test` 运行 53 个测试用例，0 Failures / 0 Errors / 0 Skipped 全部通过；
  - PC前端：`cd frontend-pc && npm run build` 0 错误构建成功；
  - 移动前端：`cd frontend-mobile && npm run build` 0 错误构建成功。
- **Commit 哈希**：`ddf40ad`
