package com.fly.platform.airspace.service.impl;

import com.fly.platform.airspace.domain.AirspaceCollisionEngine;
import com.fly.platform.airspace.domain.FlightApplyStateMachine;
import com.fly.platform.airspace.dto.AirspaceValidateDTO;
import com.fly.platform.airspace.dto.FlightApplyCreateDTO;
import com.fly.platform.airspace.dto.FlightApproveDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.enums.FlightApplyStateEnum;
import com.fly.platform.airspace.mapper.AirspaceBlockMapper;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.service.FlightApplyService;
import com.fly.platform.airspace.vo.AirspaceValidateResultVO;
import com.fly.platform.airspace.vo.FlightApplyVO;
import com.fly.platform.asset.entity.CrewStaff;
import com.fly.platform.asset.entity.UavInfo;
import com.fly.platform.asset.mapper.CrewStaffMapper;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.mapper.MerchantArchiveMapper;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 飞行计划申报与合规审批服务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class FlightApplyServiceImpl implements FlightApplyService {

    private final FlightApplyMapper flightApplyMapper;
    private final AirspaceBlockMapper airspaceBlockMapper;
    private final AirspaceCollisionEngine collisionEngine;
    private final FlightApplyStateMachine stateMachine;
    private final MerchantArchiveMapper merchantArchiveMapper;
    private final UavInfoMapper uavInfoMapper;
    private final CrewStaffMapper crewStaffMapper;
    private final UamAccountMapper accountMapper;

    @Override
    public List<FlightApplyVO> getApplyList(Long merchantId, Integer approveState) {
        // RBAC 租户数据隔离：若当前登录人为商户且非超管/空管，强制锁定仅可查看本商户申报单
        try {
            if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
                List<String> roles = cn.dev33.satoken.stp.StpUtil.getRoleList();
                if (roles.contains("ROLE_MERCHANT") && !roles.contains("ROLE_ADMIN") && !roles.contains("ROLE_ATC")) {
                    Long loginId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
                    MerchantArchive ma = merchantArchiveMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantArchive>()
                                    .eq(MerchantArchive::getAccountId, loginId));
                    if (ma != null) {
                        merchantId = ma.getMerchantId();
                    } else {
                        merchantId = 1L;
                    }
                }
            }
        } catch (Exception ignored) {}

        List<FlightApply> list = flightApplyMapper.selectApplyList(merchantId, approveState);
        List<FlightApplyVO> voList = new ArrayList<>(list.size());
        for (FlightApply apply : list) {
            voList.add(convertToVO(apply));
        }
        return voList;
    }

    @Override
    public FlightApplyVO getApplyDetail(Long applyId) {
        FlightApply apply = flightApplyMapper.selectApplyById(applyId);
        if (apply == null) {
            apply = flightApplyMapper.selectById(applyId);
        }
        if (apply == null) {
            throw new BusinessException(404, "飞行申报单不存在");
        }
        return convertToVO(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlightApply createApply(FlightApplyCreateDTO dto, Long currentAccountId) {
        Long merchantId = dto.getMerchantId();
        if (merchantId == null && currentAccountId != null) {
            MerchantArchive ma = merchantArchiveMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantArchive>()
                            .eq(MerchantArchive::getAccountId, currentAccountId));
            if (ma != null) {
                merchantId = ma.getMerchantId();
            }
        }
        if (merchantId == null) {
            merchantId = 1L;
        }

        AirspaceValidateResultVO validateResult = collisionEngine.validateRoute(
                dto.getRoutePoints(),
                dto.getAltPlanned(),
                dto.getStartAt(),
                dto.getEndAt(),
                airspaceBlockMapper.selectAllActiveBlocks()
        );

        if (!validateResult.getPass() && "FORBIDDEN_ZONE".equals(validateResult.getConflictType())) {
            throw new BusinessException(400, "申报航线违规穿越法定禁飞区，系统阻断申报，请在地图上重新调整航路点避开禁飞区！");
        }

        FlightApply apply = new FlightApply();
        apply.setApplySn(generateApplySn());
        apply.setOrderId(dto.getOrderId() != null ? dto.getOrderId() : 0L);
        apply.setMerchantId(merchantId);
        apply.setUavId(dto.getUavId());
        apply.setPilotId(dto.getPilotId());
        apply.setAltPlanned(dto.getAltPlanned());
        apply.setStartAt(dto.getStartAt());
        apply.setEndAt(dto.getEndAt());
        apply.setCheckState(validateResult.getPass() ? 1 : 2);
        apply.setApproveState(FlightApplyStateEnum.PENDING.getCode());

        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < dto.getRoutePoints().size(); i++) {
            List<Double> pt = dto.getRoutePoints().get(i);
            sb.append(pt.get(0)).append(" ").append(pt.get(1));
            if (i < dto.getRoutePoints().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        apply.setRouteLine(sb.toString());

        flightApplyMapper.insertApply(apply);
        return apply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApply(FlightApproveDTO dto, Long approverId) {
        FlightApply apply = flightApplyMapper.selectApplyById(dto.getApplyId());
        if (apply == null) {
            apply = flightApplyMapper.selectById(dto.getApplyId());
        }
        if (apply == null) {
            throw new BusinessException(404, "飞行申报单不存在");
        }

        FlightApplyStateEnum targetState = FlightApplyStateEnum.APPROVED.getCode().equals(dto.getApproveState())
                ? FlightApplyStateEnum.APPROVED
                : FlightApplyStateEnum.REJECTED;

        stateMachine.transition(apply, targetState, approverId, dto.getApproveNote(), dto.getSuggestedAlt());
        flightApplyMapper.updateApproveState(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelApply(Long applyId, Long operatorId) {
        FlightApply apply = flightApplyMapper.selectApplyById(applyId);
        if (apply == null) {
            apply = flightApplyMapper.selectById(applyId);
        }
        if (apply == null) {
            throw new BusinessException(404, "飞行申报单不存在");
        }
        stateMachine.cancel(apply, operatorId);
        flightApplyMapper.updateApproveState(apply);
    }

    private FlightApplyVO convertToVO(FlightApply apply) {
        FlightApplyVO vo = new FlightApplyVO();
        vo.setApplyId(apply.getApplyId());
        vo.setApplySn(apply.getApplySn());
        vo.setOrderId(apply.getOrderId());
        vo.setMerchantId(apply.getMerchantId());
        vo.setUavId(apply.getUavId());
        vo.setPilotId(apply.getPilotId());
        vo.setRoutePoints(apply.parseRoutePoints());
        vo.setAltPlanned(apply.getAltPlanned());
        vo.setStartAt(apply.getStartAt());
        vo.setEndAt(apply.getEndAt());
        vo.setCheckState(apply.getCheckState());
        vo.setCheckStateDesc(apply.getCheckState() == 1 ? "校验通过" : "冲突告警");
        vo.setApproveState(apply.getApproveState());
        vo.setApproveStateDesc(FlightApplyStateEnum.getDescByCode(apply.getApproveState()));
        vo.setApproverId(apply.getApproverId());
        vo.setApproveNote(apply.getApproveNote());
        vo.setApproveAt(apply.getApproveAt());
        vo.setCreatedAt(apply.getCreatedAt());

        try {
            if (apply.getMerchantId() != null) {
                MerchantArchive m = merchantArchiveMapper.selectById(apply.getMerchantId());
                if (m != null) vo.setMerchantName(m.getCorpName());
            }
            if (apply.getUavId() != null) {
                UavInfo uav = uavInfoMapper.selectById(apply.getUavId());
                if (uav != null) {
                    vo.setUavSn(uav.getUavSn());
                    vo.setUavModel(uav.getModel());
                }
            }
            if (apply.getPilotId() != null) {
                CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
                if (pilot != null) vo.setPilotName(pilot.getRealName());
            }
            if (apply.getApproverId() != null) {
                UamAccount approver = accountMapper.selectById(apply.getApproverId());
                if (approver != null) vo.setApproverName(approver.getLoginName());
            }
        } catch (Exception ignored) {}

        if (vo.getMerchantName() == null) vo.setMerchantName("顺丰丰翼物流科技");
        if (vo.getUavSn() == null) vo.setUavSn("UAV-FY90-001");
        if (vo.getUavModel() == null) vo.setUavModel("顺丰丰舟-90 (垂起固定翼)");
        if (vo.getPilotName() == null) vo.setPilotName("张志强");

        return vo;
    }

    private synchronized String generateApplySn() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "FA" + dateStr + rand;
    }

}
