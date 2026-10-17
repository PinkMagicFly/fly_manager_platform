package com.fly.platform.airspace.domain;

import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 飞行计划申报审批四态流转状态机
 * 0-待审批 -> 1-已通过 / 2-已驳回 / 3-已撤销
 *
 * @author SoarCloud Team
 */
@Component
public class FlightApplyStateMachine {

    /**
     * 执行审批决策状态跃迁
     *
     * @param apply        申报单实体
     * @param targetState  目标状态
     * @param approverId   审批人ID
     * @param approveNote  审批批复意见
     * @param suggestedAlt 修正巡航高度（可选）
     */
    public void transition(FlightApply apply, FlightApplyStateEnum targetState,
                           Long approverId, String approveNote, Integer suggestedAlt) {
        if (apply == null) {
            throw new BusinessException(404, "申报单不存在");
        }

        Integer currentState = apply.getApproveState();
        if (currentState == null) {
            currentState = FlightApplyStateEnum.PENDING.getCode();
        }

        // 仅在待审批状态允许流转到已通过或已驳回
        if (!FlightApplyStateEnum.PENDING.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("申报单当前处于【%s】状态，无法进行二次审批操作", currentDesc));
        }

        if (FlightApplyStateEnum.APPROVED.equals(targetState)) {
            apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
            apply.setApproverId(approverId);
            apply.setApproveNote(approveNote != null ? approveNote : "空管合规审核通过，准予起飞执行");
            apply.setApproveAt(LocalDateTime.now());
            if (suggestedAlt != null && suggestedAlt > 0) {
                apply.setAltPlanned(suggestedAlt);
            }
        } else if (FlightApplyStateEnum.REJECTED.equals(targetState)) {
            apply.setApproveState(FlightApplyStateEnum.REJECTED.getCode());
            apply.setApproverId(approverId);
            apply.setApproveNote(approveNote != null ? approveNote : "空域冲突驳回");
            apply.setApproveAt(LocalDateTime.now());
        } else {
            throw new BusinessException(400, "不支持的审批目标状态跃迁");
        }
    }

    /**
     * 申报人主动撤回申请
     */
    public void cancel(FlightApply apply, Long operatorId) {
        if (apply == null) {
            throw new BusinessException(404, "申报单不存在");
        }

        Integer currentState = apply.getApproveState();
        if (FlightApplyStateEnum.APPROVED.getCode().equals(currentState)) {
            throw new BusinessException(400, "已通过审批的飞行计划单据已锁定空域时段，不允许直接撤回，需联系空管中心申请变更");
        }

        if (!FlightApplyStateEnum.PENDING.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("申报单当前处于【%s】状态，无法执行撤销", currentDesc));
        }

        apply.setApproveState(FlightApplyStateEnum.CANCELED.getCode());
        apply.setApproveNote("商户主动撤回飞行计划申请");
        apply.setApproveAt(LocalDateTime.now());
    }
}
