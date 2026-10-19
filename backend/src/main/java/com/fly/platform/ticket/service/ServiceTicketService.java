package com.fly.platform.ticket.service;

import com.fly.platform.common.api.PageResult;
import com.fly.platform.ticket.dto.TicketAssignDTO;
import com.fly.platform.ticket.dto.TicketCloseDTO;
import com.fly.platform.ticket.dto.TicketCreateDTO;
import com.fly.platform.ticket.dto.TicketReplyDTO;
import com.fly.platform.ticket.vo.TicketAssigneeVO;
import com.fly.platform.ticket.vo.TicketVO;

import java.util.List;

/**
 * 客户服务与异常工单业务服务接口
 *
 * @author SoarCloud Team
 */
public interface ServiceTicketService {

    /**
     * 客户/用户创建工单
     *
     * @param dto 工单入参
     * @return 生成的工单主键ID
     */
    Long create(TicketCreateDTO dto);

    /**
     * 获取当前登录人提交的工单列表
     *
     * @return 我的工单列表
     */
    List<TicketVO> getMine();

    /**
     * 分页查询工单列表（管理端/客服端）
     *
     * @param state    状态筛选
     * @param type     类别筛选
     * @param keyword  流水号/标题关键词
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<TicketVO> page(Integer state, Integer type, String keyword, int pageNo, int pageSize);

    /**
     * 查询工单详细及完整回复流水
     *
     * @param id 工单ID
     * @return 工单详情VO
     */
    TicketVO getDetail(Long id);

    /**
     * 分派工单经办专员
     *
     * @param id  工单ID
     * @param dto 分派入参
     */
    void assign(Long id, TicketAssignDTO dto);

    /**
     * 获取可供分派的客服专员列表
     *
     * @return 客服专员列表
     */
    List<TicketAssigneeVO> getAvailableAssignees();

    /**
     * 回复工单
     *
     * @param id  工单ID
     * @param dto 回复内容
     */
    void reply(Long id, TicketReplyDTO dto);

    /**
     * 办结关闭工单
     *
     * @param id  工单ID
     * @param dto 办结入参
     */
    void close(Long id, TicketCloseDTO dto);
}
