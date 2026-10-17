package com.fly.platform.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.asset.domain.UavHealthScoreCalculator;
import com.fly.platform.asset.domain.UavStateMachine;
import com.fly.platform.asset.dto.FaultReportDTO;
import com.fly.platform.asset.dto.MaintainAcceptDTO;
import com.fly.platform.asset.dto.UavCreateUpdateDTO;
import com.fly.platform.asset.entity.BaseSite;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.entity.UavMaintain;
import com.fly.platform.asset.enums.MaintainStateEnum;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.BaseSiteMapper;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.asset.mapper.UavMaintainMapper;
import com.fly.platform.asset.service.UavService;
import com.fly.platform.asset.vo.UavDetailVO;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.mapper.MerchantArchiveMapper;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 无人机资产台账业务服务实现类
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UavServiceImpl implements UavService {

    private final UavInfoMapper uavInfoMapper;
    private final UavMaintainMapper uavMaintainMapper;
    private final BaseSiteMapper baseSiteMapper;
    private final MerchantArchiveMapper merchantArchiveMapper;

    @Override
    public List<UavDetailVO> listUavs(Long merchantId, String model, Integer state) {
        List<UavInfo> list = uavInfoMapper.selectUavList(merchantId, model, state);
        List<UavDetailVO> result = new ArrayList<>();
        for (UavInfo uav : list) {
            result.add(convertToVO(uav));
        }
        return result;
    }

    @Override
    public UavDetailVO getUavDetail(Long uavId) {
        UavInfo uav = uavInfoMapper.selectById(uavId);
        if (uav == null) {
            throw new BusinessException(404, "无人机资产不存在");
        }
        return convertToVO(uav);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UavInfo createUav(UavCreateUpdateDTO dto) {
        UavInfo exist = uavInfoMapper.selectBySn(dto.getUavSn());
        if (exist != null) {
            throw new BusinessException(400, "机身设备编号已存在: " + dto.getUavSn());
        }

        UavInfo uav = new UavInfo();
        BeanUtils.copyProperties(dto, uav);
        uav.setBatteryPct(dto.getBatteryPct() != null ? dto.getBatteryPct() : 100);
        uav.setHealthScore(100);
        uav.setFlyHours(0);
        uav.setState(UavStateEnum.IDLE.getCode());
        uav.initAudit();
        uavInfoMapper.insert(uav);
        log.info("登记新增无人机资产: SN={}, ID={}", uav.getUavSn(), uav.getUavId());
        return uav;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UavInfo updateUav(Long uavId, UavCreateUpdateDTO dto) {
        UavInfo uav = uavInfoMapper.selectById(uavId);
        if (uav == null) {
            throw new BusinessException(404, "无人机资产不存在");
        }
        if (!uav.getUavSn().equals(dto.getUavSn())) {
            UavInfo conflict = uavInfoMapper.selectBySn(dto.getUavSn());
            if (conflict != null) {
                throw new BusinessException(400, "设备编号已被占用: " + dto.getUavSn());
            }
        }
        BeanUtils.copyProperties(dto, uav);
        uav.setUavId(uavId);
        uav.updateAudit();
        uavInfoMapper.updateById(uav);
        return uav;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUavState(Long uavId, Integer targetState, String remark) {
        UavInfo uav = uavInfoMapper.selectById(uavId);
        if (uav == null) {
            throw new BusinessException(404, "无人机资产不存在");
        }

        // 状态机守卫严格校验
        UavStateMachine.validateTransition(uav.getState(), targetState);

        Integer oldState = uav.getState();
        uav.setState(targetState);
        uav.updateAudit();
        uavInfoMapper.updateById(uav);

        log.info("无人机状态流转成功: uavId={}, 原状态={}, 新状态={}, 备注={}",
                uavId, oldState, targetState, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UavMaintain reportFault(FaultReportDTO dto) {
        UavInfo uav = uavInfoMapper.selectById(dto.getUavId());
        if (uav == null) {
            throw new BusinessException(404, "无人机资产不存在");
        }

        // 1. 无人机五态联动变更为：3-故障
        uav.setState(UavStateEnum.FAULT.getCode());
        int faultCount = uavMaintainMapper.countFaultsByUavId(uav.getUavId()) + 1;
        int newScore = UavHealthScoreCalculator.calculate(
                uav.getFlyHours() != null ? uav.getFlyHours() : 0,
                faultCount,
                0,
                uav.getBatteryPct() != null ? uav.getBatteryPct() : 100
        );
        uav.setHealthScore(newScore);
        uav.updateAudit();
        uavInfoMapper.updateById(uav);

        // 2. 插入维保工单 (故障上报维修)
        UavMaintain maintain = new UavMaintain();
        maintain.setUavId(uav.getUavId());
        maintain.setMaintainType(2);
        maintain.setFaultDesc(dto.getFaultDesc());
        maintain.setMaintainState(MaintainStateEnum.PENDING.getCode());
        maintain.initAudit();
        uavMaintainMapper.insert(maintain);

        log.warn("现场提报无人机设备故障: uavId={}, SN={}, 工单ID={}",
                uav.getUavId(), uav.getUavSn(), maintain.getMaintainId());
        return maintain;
    }

    @Override
    public List<UavMaintain> listMaintains(Long uavId) {
        if (uavId != null) {
            return uavMaintainMapper.selectByUavId(uavId);
        }
        return uavMaintainMapper.selectList(new LambdaQueryWrapper<UavMaintain>()
                .orderByDesc(UavMaintain::getMaintainId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptMaintain(Long maintainId, MaintainAcceptDTO dto) {
        UavMaintain maintain = uavMaintainMapper.selectById(maintainId);
        if (maintain == null) {
            throw new BusinessException(404, "维保工单不存在");
        }
        if (maintain.getMaintainState() == MaintainStateEnum.PASSED.getCode()) {
            throw new BusinessException(400, "该工单已完成验收，无需重复操作");
        }

        maintain.setMaintainState(MaintainStateEnum.PASSED.getCode());
        maintain.setRepairContent(dto.getRepairContent());
        maintain.updateAudit();
        uavMaintainMapper.updateById(maintain);

        UavInfo uav = uavInfoMapper.selectById(maintain.getUavId());
        if (uav != null) {
            uav.setState(UavStateEnum.IDLE.getCode());
            int newScore = dto.getHealthScore() != null ? dto.getHealthScore() : 98;
            uav.setHealthScore(newScore);
            uav.updateAudit();
            uavInfoMapper.updateById(uav);
            log.info("无人机维保验收合格，已恢复闲置就绪: uavId={}, SN={}, 新健康度={}",
                    uav.getUavId(), uav.getUavSn(), newScore);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UavMaintain submitInspect(com.fly.platform.asset.dto.UavInspectDTO dto) {
        UavInfo uav = uavInfoMapper.selectById(dto.getUavId());
        if (uav == null) {
            throw new BusinessException(404, "无人机资产不存在");
        }

        // 1. 插入一条类型为 1-定期巡检保养，状态为 2-已验收合格 的工单记录
        UavMaintain maintain = new UavMaintain();
        maintain.setUavId(uav.getUavId());
        maintain.setMaintainType(1); // 1-定期巡检保养
        maintain.setFaultDesc(dto.getItemsSummary() != null ? dto.getItemsSummary() : "航前/航后例行安全巡检打卡全部合格");
        maintain.setRepairContent(dto.getInspectNote() != null ? dto.getInspectNote() : "机身外观良好、紧固螺丝锁死、动力电池接触完好、图传GPS定位正常，自检放行");
        maintain.setMaintainState(MaintainStateEnum.PASSED.getCode()); // 2-已验收合格
        maintain.initAudit();
        uavMaintainMapper.insert(maintain);

        // 2. 联动更新无人机健康评分和下次计划维保日期 (往后顺延7天)
        if (uav.getHealthScore() == null || uav.getHealthScore() < 95) {
            uav.setHealthScore(98);
        }
        uav.setNextMaintainAt(java.time.LocalDateTime.now().plusDays(7));
        uav.updateAudit();
        uavInfoMapper.updateById(uav);

        log.info("移动作业端完成无人机巡检打卡: uavId={}, SN={}, 工单ID={}",
                uav.getUavId(), uav.getUavSn(), maintain.getMaintainId());
        return maintain;
    }


    private UavDetailVO convertToVO(UavInfo uav) {
        UavDetailVO vo = new UavDetailVO();
        BeanUtils.copyProperties(uav, vo);

        UavStateEnum stateEnum = UavStateEnum.fromCode(uav.getState());
        vo.setStateDesc(stateEnum != null ? stateEnum.getDescription() : "未知");

        if (uav.getHomeSiteId() != null) {
            BaseSite site = baseSiteMapper.selectById(uav.getHomeSiteId());
            if (site != null) {
                vo.setHomeSiteName(site.getSiteName());
            }
        }
        if (uav.getMerchantId() != null) {
            MerchantArchive merchant = merchantArchiveMapper.selectById(uav.getMerchantId());
            if (merchant != null) {
                vo.setMerchantCorpName(merchant.getCorpName());
            }
        }
        return vo;
    }

}
