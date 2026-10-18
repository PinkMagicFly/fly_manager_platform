package com.fly.platform.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fly.platform.common.api.PageResult;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import com.fly.platform.ticket.dto.TicketAssignDTO;
import com.fly.platform.ticket.dto.TicketCloseDTO;
import com.fly.platform.ticket.dto.TicketCreateDTO;
import com.fly.platform.ticket.dto.TicketReplyDTO;
import com.fly.platform.ticket.service.ServiceTicketService;
import com.fly.platform.ticket.vo.TicketVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户服务与异常工单协同控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/ticket")
@RequiredArgsConstructor
public class ServiceTicketController {

    private final ServiceTicketService ticketService;

    /**
     * 客户自主提交服务工单
     */
    @PostMapping
    @SaCheckLogin
    @Audit(module = "客户服务", action = "新建服务工单")
    public Result<Long> createTicket(@Valid @RequestBody TicketCreateDTO dto) {
        return Result.ok(ticketService.create(dto));
    }

    /**
     * 获取当前客户提交的工单列表
     */
    @GetMapping("/mine")
    @SaCheckLogin
    public Result<List<TicketVO>> getMineTickets() {
        return Result.ok(ticketService.getMine());
    }

    /**
     * 管理端分页查询工单列表
     */
    @GetMapping("/page")
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    public Result<PageResult<TicketVO>> pageTickets(
            @RequestParam(required = false) Integer state,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int pageNo,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return Result.ok(ticketService.page(state, type, keyword, pageNo, pageSize));
    }

    /**
     * 查询工单全景详情及答复记录
     */
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<TicketVO> getDetail(@PathVariable Long id) {
        return Result.ok(ticketService.getDetail(id));
    }

    /**
     * 分派工单处理专员（仅超管权限）
     */
    @PutMapping("/{id}/assign")
    @SaCheckRole("ROLE_ADMIN")
    @Audit(module = "工单调度", action = "分派工单经办客服")
    public Result<Void> assignTicket(@PathVariable Long id, @Valid @RequestBody TicketAssignDTO dto) {
        ticketService.assign(id, dto);
        return Result.ok();
    }

    /**
     * 官方客服答复工单
     */
    @PutMapping("/{id}/reply")
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    @Audit(module = "客户服务", action = "官方答复服务工单")
    public Result<Void> replyTicket(@PathVariable Long id, @Valid @RequestBody TicketReplyDTO dto) {
        ticketService.reply(id, dto);
        return Result.ok();
    }

    /**
     * 办结关闭工单
     */
    @PutMapping("/{id}/close")
    @SaCheckRole(value = {"ROLE_ADMIN", "ROLE_MERCHANT"}, mode = SaMode.OR)
    @Audit(module = "客户服务", action = "办结关闭服务工单")
    public Result<Void> closeTicket(@PathVariable Long id, @RequestBody(required = false) TicketCloseDTO dto) {
        ticketService.close(id, dto);
        return Result.ok();
    }
}

