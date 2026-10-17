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
 * 飞行任务六态流转状态机单元测试 (G2/G3/G4)
 * 0待审批 -> 1已批准 / 2已驳回 / 3已取消 -> 4执行中 -> 5已完成
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
    @DisplayName("测试2：正向流转 - 待审批驳回并记录驳回理由 (G3必填校验通过)")
    public void testRejectSuccess() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(11L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        stateMachine.transition(apply, FlightApplyStateEnum.REJECTED, 3L, "航线与重大赛事临时空域冲突", null);

        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.REJECTED.getCode());
        assertThat(apply.getApproveNote()).contains("冲突");
    }

    @Test
    @DisplayName("测试3：G3驳回校验 - 驳回原因为空时异常阻断")
    public void testRejectWithoutReasonFails() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(110L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                stateMachine.transition(apply, FlightApplyStateEnum.REJECTED, 3L, "   ", null)
        );
        assertThat(ex.getMessage()).contains("驳回原因必填");
    }

    @Test
    @DisplayName("测试4：正向流转 - 待审批主动撤销")
    public void testCancelSuccess() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(12L);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        stateMachine.cancel(apply, 2L);

        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.CANCELED.getCode());
    }

    @Test
    @DisplayName("测试5：逆向拦截 - 已批准的申报单商户坚决不允许直接撤销")
    public void testApprovedCannotCancel() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(13L);
        apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> stateMachine.cancel(apply, 2L));
        assertThat(ex.getMessage()).contains("已通过审批的飞行计划单据已锁定空域时段，不允许直接撤回");
    }

    @Test
    @DisplayName("测试6：管理员终止 - 已批准的任务管理员可强制终止并释放资源")
    public void testAdminTerminateApprovedTask() {
        FlightApply apply = new FlightApply();
        apply.setApplyId(131L);
        apply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());

        stateMachine.terminate(apply, 1L, "突发军事演习紧急关闭空域");
        assertThat(apply.getApproveState()).isEqualTo(FlightApplyStateEnum.CANCELED.getCode());
        assertThat(apply.getApproveNote()).contains("突发军事演习");
    }

    @Test
    @DisplayName("测试7：G4起飞守卫 - 仅已批准任务允许跃迁至执行中，待审批起飞拦截")
    public void testTakeoffGuard() {
        // 未批准的任务起飞拦截
        FlightApply pendingApply = new FlightApply();
        pendingApply.setApplyId(15L);
        pendingApply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> stateMachine.startExecute(pendingApply));
        assertThat(ex.getMessage()).contains("起飞守卫拦截");

        // 已批准的任务正常起飞流转至4执行中
        FlightApply approvedApply = new FlightApply();
        approvedApply.setApplyId(16L);
        approvedApply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());

        stateMachine.startExecute(approvedApply);
        assertThat(approvedApply.getApproveState()).isEqualTo(FlightApplyStateEnum.EXECUTING.getCode());
    }

    @Test
    @DisplayName("测试8：G4送达守卫 - 仅执行中任务允许跃迁至已完成，其他状态拦截")
    public void testCompleteGuard() {
        // 已批准未起飞的任务送达拦截
        FlightApply approvedApply = new FlightApply();
        approvedApply.setApplyId(17L);
        approvedApply.setApproveState(FlightApplyStateEnum.APPROVED.getCode());

        BusinessException ex = assertThrows(BusinessException.class, () -> stateMachine.complete(approvedApply));
        assertThat(ex.getMessage()).contains("送达守卫拦截");

        // 执行中的任务送达正常流转至5已完成
        FlightApply executingApply = new FlightApply();
        executingApply.setApplyId(18L);
        executingApply.setApproveState(FlightApplyStateEnum.EXECUTING.getCode());

        stateMachine.complete(executingApply);
        assertThat(executingApply.getApproveState()).isEqualTo(FlightApplyStateEnum.COMPLETED.getCode());
    }

    @Test
    @DisplayName("测试9：重复审批拦截 - 已驳回或已撤销的单据不可二次审批")
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
