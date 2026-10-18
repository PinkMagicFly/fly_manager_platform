package com.fly.platform.airspace.domain;

import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 飞行任务六态流转状态机驱动器
 * 0待审批 -> 1已批准 / 2已驳回 / 3已取消 -> 4执行中 -> 5已完成
 * 严格守护低空飞行任务生命周期的正向推进与守卫拦截
 *
 * @author SoarCloud Team
 */
@Component
public class FlightApplyStateMachine {

    /**
     * 执行空管审批决策状态跃迁（待审批 -> 已批准 / 已驳回）
     *
     * @param apply        申报单实体
     * @param targetState  目标状态（已批准 / 已驳回）
     * @param approverId   审批人ID
     * @param approveNote  审批批复意见或驳回原因（驳回时必填）
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

        // 仅在待审批状态允许流转到已批准或已驳回
        if (!FlightApplyStateEnum.PENDING.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("申报单当前处于【%s】状态，无法进行二次审批操作", currentDesc));
        }

        if (FlightApplyStateEnum.APPROVED.equals(targetState)) {
            apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());
            apply.setApproverId(approverId);
            apply.setApproveNote(approveNote != null && !approveNote.trim().isEmpty() ? approveNote.trim() : "空管合规审核通过，准予起飞执行");
            apply.setApproveAt(LocalDateTime.now());
            if (suggestedAlt != null && suggestedAlt > 0) {
                apply.setAltPlanned(suggestedAlt);
            }
        } else if (FlightApplyStateEnum.REJECTED.equals(targetState)) {
            // G3 驳回原因必填（Service 层双重校验）
            if (approveNote == null || approveNote.trim().isEmpty()) {
                throw new BusinessException(400, "驳回原因必填，请详细说明驳回理由");
            }
            apply.setApproveState(FlightApplyStateEnum.REJECTED.getCode());
            apply.setApproverId(approverId);
            apply.setApproveNote(approveNote.trim());
            apply.setApproveAt(LocalDateTime.now());
        } else {
            throw new BusinessException(400, "不支持的审批目标状态跃迁: " + targetState);
        }
    }

    /**
     * 申报人主动撤回申请（商户仅限待审批状态可主动撤回）
     *
     * @param apply      申报单实体
     * @param operatorId 操作人ID
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

    /**
     * 空管/管理员强制终止已批准的飞行任务并释放资源
     *
     * @param apply      申报单实体
     * @param operatorId 操作人ID
     * @param reason     终止原因
     */
    public void terminate(FlightApply apply, Long operatorId, String reason) {
        if (apply == null) {
            throw new BusinessException(404, "申报单不存在");
        }

        Integer currentState = apply.getApproveState();
        if (!FlightApplyStateEnum.APPROVED.getCode().equals(currentState)
                && !FlightApplyStateEnum.PENDING.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("申报单当前处于【%s】状态，无法执行终止", currentDesc));
        }

        apply.setApproveState(FlightApplyStateEnum.CANCELED.getCode());
        apply.setApproveNote(reason != null && !reason.trim().isEmpty() ? reason : "管理员强制终止飞行任务");
        apply.setApproveAt(LocalDateTime.now());
    }

    /**
     * G4 起飞执行跃迁（已批准 -> 执行中）
     *
     * @param apply 申报单实体
     */
    public void startExecute(FlightApply apply) {
        if (apply == null) {
            throw new BusinessException(404, "申报单不存在");
        }

        Integer currentState = apply.getApproveState();
        if (!FlightApplyStateEnum.APPROVED.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("起飞守卫拦截：关联飞行任务状态必须为【已批准】才允许起飞执行，当前任务状态为【%s】", currentDesc));
        }

        apply.setApproveState(FlightApplyStateEnum.EXECUTING.getCode());
        apply.setApproveNote("机长已确认起飞，任务正式进入执行中");
    }

    /**
     * 任务完成送达跃迁（执行中 -> 已完成）
     *
     * @param apply 申报单实体
     */
    public void complete(FlightApply apply) {
        if (apply == null) {
            throw new BusinessException(404, "申报单不存在");
        }

        Integer currentState = apply.getApproveState();
        if (!FlightApplyStateEnum.EXECUTING.getCode().equals(currentState)) {
            String currentDesc = FlightApplyStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("送达守卫拦截：飞行任务状态必须为【执行中】才允许完成，当前任务状态为【%s】", currentDesc));
        }

        apply.setApproveState(FlightApplyStateEnum.COMPLETED.getCode());
        apply.setApproveNote("货运无人机精准降落签收，飞行任务圆满完成");


    }
}
