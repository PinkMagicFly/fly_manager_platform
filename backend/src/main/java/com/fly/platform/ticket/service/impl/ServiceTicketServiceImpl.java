package com.fly.platform.ticket.service.impl;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.entity.UamAccountRole;
import com.fly.platform.auth.entity.UamRole;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.auth.mapper.UamAccountRoleMapper;
import com.fly.platform.auth.mapper.UamRoleMapper;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.desensitize.DesensitizeUtils;
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
import com.fly.platform.ticket.vo.TicketAssigneeVO;
import com.fly.platform.ticket.vo.TicketReplyItemVO;
import com.fly.platform.ticket.vo.TicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
    private final UamRoleMapper roleMapper;
    private final UamAccountRoleMapper accountRoleMapper;
    private final StpInterface stpInterface;

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
        if (StpUtil.isLogin()) {
            List<String> callerRoles = StpUtil.getRoleList();
            if (!callerRoles.contains("ROLE_ADMIN") && !callerRoles.contains("ROLE_MERCHANT")) {
                throw new BusinessException(403, "仅商户管理人员或平台管理员有权分派工单");
            }
        }
        if (dto.getAssigneeId() == null || dto.getAssigneeId() <= 0) {
            throw new BusinessException(400, "请选择具体的经办客服专员");
        }

        UamAccount assignee = accountMapper.selectById(dto.getAssigneeId());
        if (assignee == null || assignee.getStatus() == null || assignee.getStatus() != 1) {
            throw new BusinessException(400, "指定的经办客服专员账号不存在或已被停用");
        }

        // 强守卫1：严禁分派给平台管理员（ROLE_ADMIN）
        List<String> targetRoles = stpInterface.getRoleList(assignee.getAccountId(), "login");
        if (targetRoles.contains("ROLE_ADMIN")) {
            throw new BusinessException(400, "不可将客户工单分派给平台管理员，请分派给专属客服专员");
        }

        // 强守卫2：分派的目标人员必须具备客服专员或商户协同权限
        if (!targetRoles.contains("ROLE_CS") && !targetRoles.contains("ROLE_MERCHANT")) {
            throw new BusinessException(400, "指定的人员无客服经办权限，请在企业子账号中为其分配【商户客服专员】角色");
        }

        ticket.setAssigneeId(dto.getAssigneeId());
        if (TicketStateEnum.PENDING.getCode().equals(ticket.getState())) {
            ticket.setState(TicketStateEnum.PROCESSING.getCode());
        }
        ticket.updateAudit();

        // 追加时间流水
        String targetName = assignee.getLoginName();
        appendReply(ticket, getCurrentLoginName(), "官方专员", "工单已由商户调度分派给客服专员【" + targetName + "】负责跟进处理");

        ticketMapper.updateById(ticket);
    }

    @Override
    public List<TicketAssigneeVO> getAvailableAssignees() {
        // 查找 ROLE_CS 角色
        UamRole csRole = roleMapper.selectOne(new LambdaQueryWrapper<UamRole>()
                .eq(UamRole::getRoleCode, "ROLE_CS"));
        if (csRole == null) {
            return Collections.emptyList();
        }

        // 查询拥有 ROLE_CS 的所有关联账号
        List<UamAccountRole> uars = accountRoleMapper.selectList(new LambdaQueryWrapper<UamAccountRole>()
                .eq(UamAccountRole::getRoleId, csRole.getRoleId()));
        if (uars.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> accountIds = uars.stream().map(UamAccountRole::getAccountId).distinct().collect(Collectors.toList());
        List<UamAccount> accounts = accountMapper.selectBatchIds(accountIds);

        List<TicketAssigneeVO> voList = new ArrayList<>();
        for (UamAccount acc : accounts) {
            if (acc.getStatus() == null || acc.getStatus() != 1) {
                continue;
            }
            // 严禁包含超级管理员
            List<String> roles = stpInterface.getRoleList(acc.getAccountId(), "login");
            if (roles.contains("ROLE_ADMIN")) {
                continue;
            }
            voList.add(TicketAssigneeVO.builder()
                    .accountId(acc.getAccountId())
                    .loginName(acc.getLoginName())
                    .mobile(DesensitizeUtils.maskMobile(acc.getMobile()))
                    .roleLabel("商户客服专员")
                    .build());
        }
        return voList;
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

        Long currentAccountId = 0L;
        if (StpUtil.isLogin()) {
            try {
                currentAccountId = StpUtil.getLoginIdAsLong();
            } catch (Exception ignored) {}
        }
        List<String> roles = StpUtil.isLogin() ? StpUtil.getRoleList() : Collections.emptyList();

        // 区分客户回复 与 客服/管理人员回复
        boolean isCustomer = roles.contains("ROLE_CUSTOMER")
                && !roles.contains("ROLE_ADMIN")
                && !roles.contains("ROLE_MERCHANT")
                && !roles.contains("ROLE_CS");

        if (isCustomer) {
            // 客户防越权守卫：只能回复本人提交的工单
            if (!currentAccountId.equals(ticket.getCustomerId())) {
                throw new BusinessException(403, "只能对本人提交的服务工单进行回复与补充");
            }
            // 客户追问/补充回答，状态保持原状，角色标识为“客户”
            appendReply(ticket, getCurrentLoginName(), "客户", dto.getContent());
        } else {
            // 客服/商户/管理员回复：若处于待处理状态，推进至处理中
            if (TicketStateEnum.PENDING.getCode().equals(ticket.getState())) {
                ticket.setState(TicketStateEnum.PROCESSING.getCode());
            }
            appendReply(ticket, getCurrentLoginName(), "客服专员", dto.getContent());
        }

        ticket.updateAudit();
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


