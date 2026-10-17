package com.fly.platform.airspace.service;

import com.fly.platform.airspace.dto.FlightApplyCreateDTO;
import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.vo.FlightApplyVO;

import java.util.List;

/**
 * 飞行计划申报与合规审批服务接口
 *
 * @author SoarCloud Team
 */
public interface FlightApplyService {

    /** 条件检索飞行申报单列表 */
    List<FlightApplyVO> getApplyList(Long merchantId, Integer approveState);

    /** 获取飞行计划申报单详情 */
    FlightApplyVO getApplyDetail(Long applyId);

    /** 提交新飞行计划在线申报（前置围栏合规检测） */
    FlightApply createApply(FlightApplyCreateDTO dto, Long currentMerchantId);

    /** 空管在线审批处理（通过 / 限高通过 / 驳回） */
    void approveApply(FlightApproveDTO dto, Long approverId);

    /** 申报人主动撤回申请 */
    void cancelApply(Long applyId, Long operatorId);
}
