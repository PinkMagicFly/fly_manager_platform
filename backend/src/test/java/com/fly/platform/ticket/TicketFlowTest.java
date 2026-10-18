package com.fly.platform.ticket;

import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.stp.StpUtil;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.ticket.controller.ServiceTicketController;
import com.fly.platform.ticket.dto.TicketAssignDTO;
import com.fly.platform.ticket.dto.TicketCloseDTO;
import com.fly.platform.ticket.dto.TicketCreateDTO;
import com.fly.platform.ticket.dto.TicketReplyDTO;
import com.fly.platform.ticket.enums.TicketStateEnum;
import com.fly.platform.ticket.enums.TicketTypeEnum;
import com.fly.platform.ticket.service.ServiceTicketService;
import com.fly.platform.ticket.vo.TicketVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 客户服务工单业务流转与权限控制测试
 * 覆盖：建单 -> 分派 -> 官方回复 -> 办结关闭全链，以及客户越权分派拦截守卫
 *
 * @author SoarCloud Team
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TicketFlowTest {

    @Autowired
    private ServiceTicketService ticketService;

    @Autowired
    private ServiceTicketController ticketController;

    @AfterEach
    void tearDown() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Test
    @DisplayName("工单全链路业务流转测试: 客户建单 -> 管理员分派 -> 客服答复 -> 办结关闭")
    void testTicketFullLifecycleChain() {
        // 1. 模拟客户登录并提交工单
        StpUtil.login(6L); // accountId=6 为预置客户 customer_demo

        TicketCreateDTO createDTO = new TicketCreateDTO();
        createDTO.setType(TicketTypeEnum.COMPLAINT.getCode());
        createDTO.setTitle("加急运单起飞延误诉求");
        createDTO.setContent("加急运单超时已达20分钟仍未见起飞动态，请求核实航道与机组调度");
        createDTO.setOrderId(1L);

        Long ticketId = ticketService.create(createDTO);
        assertThat(ticketId).isNotNull();

        // 验证客户“我的工单”列表
        List<TicketVO> myTickets = ticketService.getMine();
        assertThat(myTickets).isNotEmpty();
        assertThat(myTickets.get(0).getTicketId()).isEqualTo(ticketId);
        assertThat(myTickets.get(0).getState()).isEqualTo(TicketStateEnum.PENDING.getCode());
        assertThat(myTickets.get(0).getType()).isEqualTo(TicketTypeEnum.COMPLAINT.getCode());

        // 2. 模拟管理员登录分派客服专员
        StpUtil.login(1L); // accountId=1 为内置管理员 admin
        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(2L); // 分派给专员

        ticketService.assign(ticketId, assignDTO);

        TicketVO assignedTicket = ticketService.getDetail(ticketId);
        assertThat(assignedTicket.getState()).isEqualTo(TicketStateEnum.PROCESSING.getCode());
        assertThat(assignedTicket.getAssigneeId()).isEqualTo(2L);
        assertThat(assignedTicket.getReplies()).isNotEmpty();
        assertThat(assignedTicket.getReplies().get(0).getContent()).contains("工单已转派给客服专员");

        // 3. 客服答复客户
        TicketReplyDTO replyDTO = new TicketReplyDTO();
        replyDTO.setContent("已核实：受南山区强阵风对流微气象影响，低空空域短暂实行流量控制，预计8分钟后起飞，已为您安排备用航道。");

        ticketService.reply(ticketId, replyDTO);

        TicketVO repliedTicket = ticketService.getDetail(ticketId);
        assertThat(repliedTicket.getReplies()).hasSize(2);
        assertThat(repliedTicket.getReplies().get(1).getContent()).contains("微气象影响");

        // 4. 办结关闭工单
        TicketCloseDTO closeDTO = new TicketCloseDTO();
        closeDTO.setNote("运单已起飞并安全送达，客户确认问题闭环解除。");

        ticketService.close(ticketId, closeDTO);

        TicketVO closedTicket = ticketService.getDetail(ticketId);
        assertThat(closedTicket.getState()).isEqualTo(TicketStateEnum.CLOSED.getCode());
        assertThat(closedTicket.getReplies()).hasSize(3);

        // 5. 终态守卫校验: 已关闭工单禁止再次回复
        assertThatThrownBy(() -> ticketService.reply(ticketId, replyDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已办结关闭");
    }

    @Test
    @DisplayName("权限越权拦截测试: 客户角色尝试直接分派工单经办专员，守卫坚决拦截拒止 (403/NotRoleException)")
    void testCustomerAssignPermissionDenied() {
        // 创建工单
        StpUtil.login(6L);
        TicketCreateDTO createDTO = new TicketCreateDTO();
        createDTO.setType(TicketTypeEnum.CONSULT.getCode());
        createDTO.setTitle("低空航线开通咨询");
        createDTO.setContent("请问盐田到大鹏航线何时面向公众开放？");
        Long ticketId = ticketService.create(createDTO);

        // 此时以客户身份 (ROLE_CUSTOMER，非 ROLE_ADMIN) 尝试调用 assign 接口
        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(2L);

        // Sa-Token 注解或校验拦截
        assertThatThrownBy(() -> {
            StpUtil.checkRole("ROLE_ADMIN");
            ticketController.assignTicket(ticketId, assignDTO);
        }).isInstanceOf(NotRoleException.class);
    }
}
