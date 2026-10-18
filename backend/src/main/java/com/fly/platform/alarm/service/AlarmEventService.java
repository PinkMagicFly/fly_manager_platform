package com.fly.platform.alarm.service;

import com.fly.platform.alarm.dto.AlarmCreateDTO;
import com.fly.platform.alarm.dto.AlarmProcessDTO;
import com.fly.platform.alarm.dto.AlarmReportDTO;
import com.fly.platform.alarm.vo.AlarmVO;
import com.fly.platform.common.api.PageResult;

import java.util.Map;

/**
 * 实时告警事件业务服务接口
 *
 * @author SoarCloud Team
 */
public interface AlarmEventService {

    /**
     * 分页查询告警事件列表
     *
     * @param level    级别筛选
     * @param state    状态筛选
     * @param keyword  流水号/标题关键词
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    PageResult<AlarmVO> page(Integer level, Integer state, String keyword, int pageNo, int pageSize);

    /**
     * 查询告警事件全生命周期详情
     *
     * @param id 告警ID
     * @return 告警详情VO
     */
    AlarmVO getDetail(Long id);

    /**
     * 手工登记告警事件
     *
     * @param dto 登记入参
     * @return 生成的告警主键ID
     */
    Long createManual(AlarmCreateDTO dto);

    /**
     * 处置告警（0未处置 -> 1处置中）
     *
     * @param id  告警ID
     * @param dto 处置方案
     */
    void process(Long id, AlarmProcessDTO dto);

    /**
     * 办结关闭告警（1处置中 -> 2已关闭）
     *
     * @param id  告警ID
     * @param dto 办结解除说明
     */
    void close(Long id, AlarmProcessDTO dto);

    /**
     * 机长现场突发故障上报（联动生成严重告警、订单转80异常中断、无人机转3故障）
     *
     * @param dto 上报入参
     * @return 联动结果摘要
     */
    Map<String, Object> reportFault(AlarmReportDTO dto);
}
