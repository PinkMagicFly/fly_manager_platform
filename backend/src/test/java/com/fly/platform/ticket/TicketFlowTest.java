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
import com.fly.platform.ticket.vo.TicketAssigneeVO;
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

        // 2. 模拟商户总账号登录分派给专属客服专员 cs_sf (accountId=10)
        StpUtil.login(2L); // accountId=2 为商户主账号 merchant_sf
        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(10L); // 分派给客服专员 cs_sf

        ticketService.assign(ticketId, assignDTO);

        TicketVO assignedTicket = ticketService.getDetail(ticketId);
        assertThat(assignedTicket.getState()).isEqualTo(TicketStateEnum.PROCESSING.getCode());
        assertThat(assignedTicket.getAssigneeId()).isEqualTo(10L);
        assertThat(assignedTicket.getReplies()).isNotEmpty();
        assertThat(assignedTicket.getReplies().get(0).getContent()).contains("工单已由商户调度分派给客服专员");

        // 3. 客服答复客户
        StpUtil.login(10L); // accountId=10 为客服专员 cs_sf
        TicketReplyDTO replyDTO = new TicketReplyDTO();
        replyDTO.setContent("已核实：受南山区强阵风对流微气象影响，低空空域短暂实行流量控制，预计8分钟后起飞，已为您安排备用航道。");

        ticketService.reply(ticketId, replyDTO);

        TicketVO repliedTicket = ticketService.getDetail(ticketId);
        assertThat(repliedTicket.getReplies()).hasSize(2);
        assertThat(repliedTicket.getReplies().get(1).getRole()).isEqualTo("客服专员");
        assertThat(repliedTicket.getReplies().get(1).getContent()).contains("微气象影响");

        // 4. 客户移动端查看并继续回答追问
        StpUtil.login(6L); // 切换回客户身份
        TicketReplyDTO followUpDTO = new TicketReplyDTO();
        followUpDTO.setContent("感谢客服答复，请问改走备用航道预计最终到达时间会延误多少分钟？");

        ticketService.reply(ticketId, followUpDTO);

        TicketVO customerRepliedTicket = ticketService.getDetail(ticketId);
        assertThat(customerRepliedTicket.getReplies()).hasSize(3);
        assertThat(customerRepliedTicket.getReplies().get(2).getRole()).isEqualTo("客户");
        assertThat(customerRepliedTicket.getReplies().get(2).getContent()).contains("延误多少分钟");

        // 5. 客服专员再次跟进答复
        StpUtil.login(10L);
        TicketReplyDTO replyDTO2 = new TicketReplyDTO();
        replyDTO2.setContent("经测算航程仅增加1.2公里，耗时约增加2分钟，预计在承诺时间内送达。");

        ticketService.reply(ticketId, replyDTO2);

        TicketVO repliedTicket2 = ticketService.getDetail(ticketId);
        assertThat(repliedTicket2.getReplies()).hasSize(4);

        // 6. 办结关闭工单
        TicketCloseDTO closeDTO = new TicketCloseDTO();
        closeDTO.setNote("运单已起飞并安全送达，客户确认问题闭环解除。");

        ticketService.close(ticketId, closeDTO);

        TicketVO closedTicket = ticketService.getDetail(ticketId);
        assertThat(closedTicket.getState()).isEqualTo(TicketStateEnum.CLOSED.getCode());
        assertThat(closedTicket.getReplies()).hasSize(5);

        // 7. 终态守卫校验: 已关闭工单禁止再次回复
        assertThatThrownBy(() -> ticketService.reply(ticketId, replyDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已办结关闭");
    }

    @Test
    @DisplayName("安全守卫测试: 严禁将工单分派给平台管理员 (抛出业务拒绝)")
    void testAssignToAdminForbidden() {
        // 创建工单
        StpUtil.login(6L);
        TicketCreateDTO createDTO = new TicketCreateDTO();
        createDTO.setType(TicketTypeEnum.CONSULT.getCode());
        createDTO.setTitle("航线咨询");
        createDTO.setContent("咨询大运中心至南山直飞业务");
        Long ticketId = ticketService.create(createDTO);

        // 商户总账号登录尝试分派给系统超级管理员 admin (accountId=1)
        StpUtil.login(2L);
        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(1L); // 目标为超级管理员

        assertThatThrownBy(() -> ticketService.assign(ticketId, assignDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不可将客户工单分派给平台管理员");
    }

    @Test
    @DisplayName("客服人员列表接口测试: 能够获取 ROLE_CS 客服专员，且严格排除管理员")
    void testGetAvailableAssignees() {
        StpUtil.login(2L); // 商户主账号登录
        List<TicketAssigneeVO> assignees = ticketService.getAvailableAssignees();

        assertThat(assignees).isNotEmpty();
        // 验证包含客服专员 cs_sf (accountId=10)
        assertThat(assignees.stream().anyMatch(a -> a.getAccountId().equals(10L))).isTrue();
        // 验证坚决不包含系统管理员 admin (accountId=1)
        assertThat(assignees.stream().anyMatch(a -> a.getAccountId().equals(1L))).isFalse();
    }

    @Test
    @DisplayName("权限越权拦截测试: 客户角色尝试直接分派工单经办专员，守卫坚决拦截拒止 (403/BusinessException)")
    void testCustomerAssignPermissionDenied() {
        // 创建工单
        StpUtil.login(6L);
        TicketCreateDTO createDTO = new TicketCreateDTO();
        createDTO.setType(TicketTypeEnum.CONSULT.getCode());
        createDTO.setTitle("低空航线开通咨询");
        createDTO.setContent("请问盐田到大鹏航线何时面向公众开放？");
        Long ticketId = ticketService.create(createDTO);

        // 此时以客户身份 (ROLE_CUSTOMER，非 ROLE_ADMIN / ROLE_MERCHANT) 尝试调用 assign 接口
        TicketAssignDTO assignDTO = new TicketAssignDTO();
        assignDTO.setAssigneeId(10L);

        // 双层拦截守卫：Service 层与 Sa-Token 角色守卫
        assertThatThrownBy(() -> {
            ticketController.assignTicket(ticketId, assignDTO);
        }).isInstanceOf(BusinessException.class)
          .hasMessageContaining("仅商户管理人员或平台管理员有权分派工单");
    }
}
