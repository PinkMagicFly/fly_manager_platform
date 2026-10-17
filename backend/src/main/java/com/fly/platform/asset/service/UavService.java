package com.fly.platform.asset.service;

import com.fly.platform.asset.dto.FaultReportDTO;
import com.fly.platform.asset.dto.MaintainAcceptDTO;
import com.fly.platform.asset.dto.UavCreateUpdateDTO;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.entity.UavMaintain;
import com.fly.platform.asset.vo.UavDetailVO;

import java.util.List;

/**
 * 无人机资产台账业务服务接口
 *
 * @author SoarCloud Team
 */
public interface UavService {

    /** 条件检索无人机资产列表 */
    List<UavDetailVO> listUavs(Long merchantId, String model, Integer state);

    /** 获取无人机全景详情 */
    UavDetailVO getUavDetail(Long uavId);

    /** 登记新增无人机 */
    UavInfo createUav(UavCreateUpdateDTO dto);

    /** 编辑更新无人机资产档案 */
    UavInfo updateUav(Long uavId, UavCreateUpdateDTO dto);

    /** 驱动五态流转跃迁（受状态机守卫统一校验） */
    void changeUavState(Long uavId, Integer targetState, String remark);

    /** 外场现场故障上报（联动变更无人机为故障并生成维保工单） */
    UavMaintain reportFault(FaultReportDTO dto);

    /** 查询维保工单记录列表 */
    List<UavMaintain> listMaintains(Long uavId);

    /** 维保验收合格（联动恢复无人机为闲置） */
    /** 移动作业端巡检打卡（落库定期巡检合格工单并更新下次保养时间） */
    UavMaintain submitInspect(com.fly.platform.asset.dto.UavInspectDTO dto);

    void acceptMaintain(Long maintainId, MaintainAcceptDTO dto);
}
