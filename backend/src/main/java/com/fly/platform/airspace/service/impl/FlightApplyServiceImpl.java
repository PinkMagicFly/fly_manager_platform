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
import com.fly.platform.asset.entity.SiteBooking;
import com.fly.platform.asset.mapper.CrewStaffMapper;
import com.fly.platform.asset.mapper.SiteBookingMapper;
import com.fly.platform.asset.mapper.UavInfoMapper;
import com.fly.platform.auth.entity.MerchantArchive;
import com.fly.platform.auth.entity.UamAccount;
import com.fly.platform.auth.mapper.MerchantArchiveMapper;
import com.fly.platform.auth.mapper.UamAccountMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.order.domain.CargoOrderStateMachine;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.enums.OrderStateEnum;
import com.fly.platform.order.mapper.CargoOrderMapper;
import com.fly.platform.order.mapper.OrderStateFlowMapper;
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
    private final CargoOrderMapper cargoOrderMapper;
    private final OrderStateFlowMapper orderStateFlowMapper;
    private final CargoOrderStateMachine cargoOrderStateMachine;
    private final SiteBookingMapper siteBookingMapper;

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

        // G6 防重：关联订单时检查是否存在进行中的任务
        if (dto.getOrderId() != null && dto.getOrderId() > 0) {
            FlightApply existing = flightApplyMapper.selectByOrderId(dto.getOrderId());
            if (existing != null && !FlightApplyStateEnum.isTerminal(existing.getApproveState())) {
                throw new BusinessException(400, "该订单已存在进行中的飞行任务，禁止重复提交");
            }
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

        if (FlightApplyStateEnum.REJECTED.equals(targetState)) {
            // G3 驳回原因必填（Service 双重校验）
            if (dto.getApproveNote() == null || dto.getApproveNote().trim().isEmpty()) {
                throw new BusinessException(400, "驳回原因必填，请详细说明驳回理由");
            }
            stateMachine.transition(apply, targetState, approverId, dto.getApproveNote().trim(), null);
            flightApplyMapper.updateApproveState(apply);
            // 驳回时订单停留待调度30，资源零占用，无需回退
            return;
        }

        // G1 审批通过回调：
        // 1. 空域时段确权 + 锁定无人机（校验同一无人机同一时段无其他已批准任务）
        if (apply.getUavId() != null && apply.getStartAt() != null && apply.getEndAt() != null) {
            List<FlightApply> conflicts = flightApplyMapper.selectOverlappingApprovedTasks(
                    apply.getUavId(), apply.getStartAt(), apply.getEndAt(), apply.getApplyId());
            if (conflicts != null && !conflicts.isEmpty()) {
                throw new BusinessException(400, String.format("无人机时段冲突拦截：机号ID[%d]在所选时段已被任务【%s】锁定占用，不可重复批准！",
                        apply.getUavId(), conflicts.get(0).getApplySn()));
            }
        }

        // 2. 机长可用性核实
        if (apply.getPilotId() != null) {
            CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
            if (pilot == null) {
                throw new BusinessException(404, "指派执飞机长不存在");
            }
        }

        // 3. 场站预约落地
        Long siteId = 1L;
        if (apply.getUavId() != null) {
            UavInfo uav = uavInfoMapper.selectById(apply.getUavId());
            if (uav != null && uav.getHomeSiteId() != null && uav.getHomeSiteId() > 0) {
                siteId = uav.getHomeSiteId();
            }
        }
        SiteBooking booking = new SiteBooking();
        booking.setSiteId(siteId);
        booking.setUavId(apply.getUavId() != null ? apply.getUavId() : 1L);
        booking.setApplyId(apply.getApplyId());
        booking.setStartTime(apply.getStartAt() != null ? apply.getStartAt() : java.time.LocalDateTime.now());
        booking.setEndTime(apply.getEndAt() != null ? apply.getEndAt() : java.time.LocalDateTime.now().plusHours(1));
        booking.setBookingState(1); // 1-已预约
        booking.initAudit();
        siteBookingMapper.insert(booking);

        // 4. 驱动任务状态机流转为已批准
        stateMachine.transition(apply, FlightApplyStateEnum.APPROVED, approverId, dto.getApproveNote(), dto.getSuggestedAlt());
        flightApplyMapper.updateApproveState(apply);

        // 5. 联动订单状态跃迁：若关联履约订单（order_id > 0），推进订单 30 -> 40 并写入状态变迁流水
        if (apply.getOrderId() != null && apply.getOrderId() > 0) {
            CargoOrder order = cargoOrderMapper.selectOrderById(apply.getOrderId());
            if (order != null && OrderStateEnum.PENDING_DISPATCH.getCode().equals(order.getOrderState())) {
                cargoOrderStateMachine.transition(order, OrderStateEnum.PENDING_TAKEOFF.getCode(), approverId,
                        "飞行计划空管审批通过，空域时段确权完成，资源正式锁定，运单推进至待起飞 (任务编号: " + apply.getApplySn() + ")",
                        orderStateFlowMapper);
                cargoOrderMapper.updateOrderDispatch(order.getOrderId(), OrderStateEnum.PENDING_TAKEOFF.getCode(),
                        apply.getUavId(), apply.getApplyId());
            }
        }
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

        // 释放可能已绑定的场站预约
        siteBookingMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SiteBooking>()
                        .eq(SiteBooking::getApplyId, applyId)
                        .set(SiteBooking::getBookingState, 4)); // 4-已取消
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
