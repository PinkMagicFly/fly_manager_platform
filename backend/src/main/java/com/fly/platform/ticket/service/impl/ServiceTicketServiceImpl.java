package com.fly.platform.ticket.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.ticket.dto.TicketAssignDTO;
import com.fly.platform.ticket.dto.TicketCloseDTO;
import com.fly.platform.ticket.dto.TicketCreateDTO;
import com.fly.platform.ticket.dto.TicketReplyDTO;
import com.fly.platform.ticket.entity.ServiceTicket;
import com.fly.platform.ticket.enums.TicketStateEnum;
import com.fly.platform.ticket.enums.TicketTypeEnum;
import com.fly.platform.ticket.mapper.ServiceTicketMapper;
import com.fly.platform.ticket.service.ServiceTicketService;
import com.fly.platform.ticket.vo.TicketReplyItemVO;
import com.fly.platform.ticket.vo.TicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户服务与异常工单业务实现
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketServiceImpl implements ServiceTicketService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ServiceTicketMapper ticketMapper;
    private final UamAccountMapper accountMapper;
    private final CargoOrderMapper cargoOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketCreateDTO dto) {
        Long accountId = 0L;
        if (StpUtil.isLogin()) {
            try {
                accountId = StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }

        String sn = "TCK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + (int) (Math.random() * 9000 + 1000);

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketSn(sn);
        ticket.setCustomerId(accountId);
        ticket.setOrderId(dto.getOrderId());
        ticket.setType(dto.getType());
        ticket.setTitle(dto.getTitle());
        ticket.setContent(dto.getContent());
        ticket.setState(TicketStateEnum.PENDING.getCode());
        ticket.setReplies("[]");
        ticket.initAudit();

        ticketMapper.insert(ticket);
        log.info("用户提交客服工单: ticketId={}, ticketSn={}", ticket.getTicketId(), ticket.getTicketSn());
        return ticket.getTicketId();
    }

    @Override
    public List<TicketVO> getMine() {
        Long accountId = 0L;
        if (StpUtil.isLogin()) {
            try {
                accountId = StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }

        List<ServiceTicket> list = ticketMapper.selectList(
                new LambdaQueryWrapper<ServiceTicket>()
                        .eq(ServiceTicket::getCustomerId, accountId)
                        .orderByDesc(ServiceTicket::getTicketId)
        );
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<TicketVO> page(Integer state, Integer type, String keyword, int pageNo, int pageSize) {
        LambdaQueryWrapper<ServiceTicket> qw = new LambdaQueryWrapper<>();
        if (state != null && state >= 0) {
            qw.eq(ServiceTicket::getState, state);
        }
        if (type != null && type > 0) {
            qw.eq(ServiceTicket::getType, type);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.and(w -> w.like(ServiceTicket::getTicketSn, keyword.trim())
                    .or().like(ServiceTicket::getTitle, keyword.trim())
                    .or().like(ServiceTicket::getContent, keyword.trim()));
        }
        qw.orderByDesc(ServiceTicket::getTicketId);

        Page<ServiceTicket> page = ticketMapper.selectPage(new Page<>(pageNo, pageSize), qw);
        List<TicketVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        return new PageResult<>(voList, page.getTotal(), (long) pageNo, (long) pageSize);
    }

    @Override
    public TicketVO getDetail(Long id) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单记录不存在: " + id);
        }
        return convertToVO(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, TicketAssignDTO dto) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单记录不存在: " + id);
        }
        ticket.setAssigneeId(dto.getAssigneeId());
        if (TicketStateEnum.PENDING.getCode().equals(ticket.getState())) {
            ticket.setState(TicketStateEnum.PROCESSING.getCode());
        }
        ticket.updateAudit();

        // 追加时间流水
        UamAccount assignee = accountMapper.selectById(dto.getAssigneeId());
        String targetName = assignee != null ? assignee.getLoginName() : "专员#" + dto.getAssigneeId();
        appendReply(ticket, getCurrentLoginName(), "官方专员", "工单已转派给客服专员【" + targetName + "】负责跟进处理");

        ticketMapper.updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reply(Long id, TicketReplyDTO dto) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单记录不存在: " + id);
        }
        if (TicketStateEnum.CLOSED.getCode().equals(ticket.getState())) {
            throw new BusinessException(400, "该工单已办结关闭，无法继续回复留言");
        }

        if (TicketStateEnum.PENDING.getCode().equals(ticket.getState())) {
            ticket.setState(TicketStateEnum.PROCESSING.getCode());
        }
        ticket.updateAudit();

        appendReply(ticket, getCurrentLoginName(), "客服专员", dto.getContent());
        ticketMapper.updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id, TicketCloseDTO dto) {
        ServiceTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单记录不存在: " + id);
        }
        if (TicketStateEnum.CLOSED.getCode().equals(ticket.getState())) {
            throw new BusinessException(400, "该工单已处于办结关闭状态，无需重复关闭");
        }

        ticket.setState(TicketStateEnum.CLOSED.getCode());
        ticket.updateAudit();

        String note = (dto != null && dto.getNote() != null && !dto.getNote().trim().isEmpty())
                ? dto.getNote().trim() : "诉求已得到妥善解决并办结";
        appendReply(ticket, getCurrentLoginName(), "官方客服", "【办结关闭】" + note);
        ticketMapper.updateById(ticket);
    }

    private void appendReply(ServiceTicket ticket, String by, String role, String content) {
        List<TicketReplyItemVO> list = parseReplies(ticket.getReplies());
        TicketReplyItemVO item = new TicketReplyItemVO();
        item.setAt(LocalDateTime.now().format(DATE_FORMATTER));
        item.setBy(by);
        item.setRole(role);
        item.setContent(content);
        list.add(item);

        try {
            ticket.setReplies(OBJECT_MAPPER.writeValueAsString(list));
        } catch (Exception e) {
            log.error("序列化工单回复失败: {}", e.getMessage());
        }
    }

    private List<TicketReplyItemVO> parseReplies(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<TicketReplyItemVO>>() {});
        } catch (Exception e) {
            log.warn("反序列化工单答复流水失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private TicketVO convertToVO(ServiceTicket t) {
        TicketVO vo = new TicketVO();
        vo.setTicketId(t.getTicketId());
        vo.setTicketSn(t.getTicketSn());
        vo.setCustomerId(t.getCustomerId());
        vo.setOrderId(t.getOrderId());
        vo.setType(t.getType());
        vo.setTypeLabel(TicketTypeEnum.getDescByCode(t.getType()));
        vo.setTitle(t.getTitle());
        vo.setContent(t.getContent());
        vo.setState(t.getState());
        vo.setStateLabel(TicketStateEnum.getDescByCode(t.getState()));
        vo.setAssigneeId(t.getAssigneeId());
        vo.setReplies(parseReplies(t.getReplies()));
        vo.setCreatedAt(t.getCreatedAt());
        vo.setUpdatedAt(t.getUpdatedAt());

        if (t.getCustomerId() != null && t.getCustomerId() > 0) {
            UamAccount cust = accountMapper.selectById(t.getCustomerId());
            if (cust != null) vo.setCustomerName(cust.getLoginName());
        }
        if (t.getAssigneeId() != null && t.getAssigneeId() > 0) {
            UamAccount ass = accountMapper.selectById(t.getAssigneeId());
            if (ass != null) vo.setAssigneeName(ass.getLoginName());
        }
        if (t.getOrderId() != null && t.getOrderId() > 0) {
            CargoOrder order = cargoOrderMapper.selectById(t.getOrderId());
            if (order != null) vo.setOrderSn(order.getOrderSn());
        }
        return vo;
    }

    private String getCurrentLoginName() {
        if (StpUtil.isLogin()) {
            try {
                Object name = StpUtil.getSession().get("loginName");
                if (name != null) return name.toString();
                return "user_" + StpUtil.getLoginId();
            } catch (Exception ignored) {}
        }
        return "官方客服";
    }
}


