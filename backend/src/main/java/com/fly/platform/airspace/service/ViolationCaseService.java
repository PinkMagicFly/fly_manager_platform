package com.fly.platform.airspace.service;

import com.fly.platform.airspace.dto.ViolationHandleDTO;
import com.fly.platform.airspace.vo.ViolationCaseVO;
import com.fly.platform.airspace.vo.ViolationDetailVO;

import java.util.List;

/**
 * 飞行违规案件调查与定责处罚服务接口
 *
 * @author SoarCloud Team
 */
public interface ViolationCaseService {

    /** 条件检索违规案件列表 */
    List<ViolationCaseVO> getCaseList(Integer violateKind, Integer punishState);

    /** 获取违规案件详情（含计划与实际偏航轨迹对比数据） */
    ViolationDetailVO getCaseDetail(Long caseId);

    /** 空管定责并下达行政处罚决定书 */
    void handleCase(ViolationHandleDTO dto, Long handlerId);
}
