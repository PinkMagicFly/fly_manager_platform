package com.fly.platform.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.ticket.entity.ServiceTicket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户服务工单持久化 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface ServiceTicketMapper extends BaseMapper<ServiceTicket> {
}
