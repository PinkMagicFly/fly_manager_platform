package com.fly.platform.asset.domain;

import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.common.exception.BusinessException;

/**
 * 无人机设备五态模型流转状态机守卫
 * 严格防范非法跃迁（如故障/维保设备直接接单执飞）
 *
 * @author SoarCloud Team
 */
public class UavStateMachine {

    /**
     * 校验五态流转合法性
     *
     * @param currentState 当前状态码
     * @param targetState 目标状态码
     * @throws BusinessException 当尝试非法跃迁时抛出
     */
    public static void validateTransition(Integer currentState, Integer targetState) {
        if (currentState == null || targetState == null) {
            throw new BusinessException(400, "状态机参数错误: 当前状态或目标状态不可为空");
        }

        if (currentState.equals(targetState)) {
            return; // 相同状态无需重复流转
        }

        UavStateEnum current = UavStateEnum.fromCode(currentState);
        UavStateEnum target = UavStateEnum.fromCode(targetState);

        if (current == null || target == null) {
            throw new BusinessException(400, "未知的无人机状态编码");
        }

        // 核心安全红线：维保中(2)或故障(3)绝对禁止直接跃迁至任务中(1)
        if ((current == UavStateEnum.MAINTAINING || current == UavStateEnum.FAULT)
                && target == UavStateEnum.ON_TASK) {
            throw new BusinessException(400, "维保中或故障设备不可直接跃迁至任务中");
        }

        switch (current) {
            case IDLE:
                // 闲置状态可转为：任务中、维保中、故障、离线
                break;
            case ON_TASK:
                // 任务中可转为：闲置（完成送达）、故障（在途突发故障）
                if (target != UavStateEnum.IDLE && target != UavStateEnum.FAULT) {
                    throw new BusinessException(400, "执行任务中的设备不可直接变更为 " + target.getDescription());
                }
                break;
            case MAINTAINING:
                // 维保中只允许验收合格后转为闲置
                if (target != UavStateEnum.IDLE) {
                    throw new BusinessException(400, "维保中的设备仅在验收合格后方可恢复为闲置");
                }
                break;
            case FAULT:
                // 故障设备只允许转入维保中维修
                if (target != UavStateEnum.MAINTAINING) {
                    throw new BusinessException(400, "故障设备必须先行登记转入维保维修");
                }
                break;
            case OFFLINE:
                // 离线设备恢复上线转为闲置
                if (target != UavStateEnum.IDLE) {
                    throw new BusinessException(400, "离线设备重新建立通信连接后应恢复为闲置状态");
                }
                break;
            default:
                throw new BusinessException(400, "非法的状态跃迁操作");
        }
    }
}
