package com.fly.platform.airspace;

import com.fly.platform.airspace.domain.FlightApplyStateMachine;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 飞行计划申报四态流转状态机单元测试
 * 0-待审批 -> 1-已通过 / 2-已驳回 / 3-已撤销
 *
 * @author SoarCloud Team
 */
public class FlightApplyStateMachineTest {

    private final FlightApplyStateMachine stateMachine = new FlightApplyStateMachine();

    @Test
    @DisplayName("测试1：正向流转 - 待审批通过并记录审批人与建议")
    public void testApproveSuccess() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(10L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());
        apply.setAltPlanned(100);

        stateMachine.transition(apply, FlightApplyStateEnum.APPROVED, 3L, "准予执行", 90);

        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.APPROVED.getCode());
        assertThat(apply.getApproverId()).isEqualTo(3L);
        assertThat(apply.getApproveNote()).isEqualTo("准予执行");
        assertThat(apply.getAltPlanned()).isEqualTo(90); // 限高修正为90米
        assertThat(apply.getApproveAt()).isNotNull();
    }

    @Test
    @DisplayName("测试2：正向流转 - 待审批驳回并记录驳回理由")
    public void testRejectSuccess() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(11L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        stateMachine.transition(apply, FlightApplyStateEnum.REJECTED, 3L, "航线与重大赛事临时空域冲突", null);

        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.REJECTED.getCode());
        assertThat(apply.getApproveNote()).contains("冲突");
    }

    @Test
    @DisplayName("测试3：正向流转 - 待审批主动撤销")
    public void testCancelSuccess() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(12L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        stateMachine.cancel(apply, 2L);

        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.CANCELED.getCode());
    }

    @Test
    @DisplayName("测试4：逆向拦截 - 已通过的申报单坚决不允许直接撤销")
    public void testApprovedCannotCancel() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(13L);
        apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> stateMachine.cancel(apply, 2L));
        assertThat(ex.getMessage()).contains("已通过审批的飞行计划单据已锁定空域时段，不允许直接撤回");
    }

    @Test
    @DisplayName("测试5：重复审批拦截 - 已驳回或已撤销的单据不可二次审批")
    public void testTerminalStateCannotReApprove() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(14L);
        apply.setApproveState(FlightApplyStateEnum.REJECTED.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.transition(apply, FlightApplyStateEnum.APPROVED, 3L, "重新通过", null)
        );
        assertThat(ex.getMessage()).contains("无法进行二次审批");
    }
}
