package com.fly.platform.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.platform.alarm.entity.AlarmEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警事件持久化 Mapper
 *
 * @author SoarCloud Team
 */
@Mapper
public interface AlarmEventMapper extends BaseMapper<AlarmEvent> {
}
