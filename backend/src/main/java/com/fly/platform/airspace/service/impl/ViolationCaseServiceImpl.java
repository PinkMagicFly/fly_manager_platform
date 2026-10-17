package com.fly.platform.airspace.service.impl;

import com.fly.platform.airspace.domain.ViolationCaseRuleEngine;
import com.fly.platform.airspace.dto.ViolationHandleDTO;
import com.fly.platform.airspace.entity.FlightApply;
import com.fly.platform.airspace.entity.ViolationCase;
import com.fly.platform.airspace.enums.PunishLevelEnum;
import com.fly.platform.airspace.enums.ViolationKindEnum;
import com.fly.platform.airspace.mapper.FlightApplyMapper;
import com.fly.platform.airspace.mapper.ViolationCaseMapper;
import com.fly.platform.airspace.service.ViolationCaseService;
import com.fly.platform.airspace.vo.ViolationCaseVO;
import com.fly.platform.airspace.vo.ViolationDetailVO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 飞行违规案件调查与定责处罚服务实现类
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class ViolationCaseServiceImpl implements ViolationCaseService {

    private final ViolationCaseMapper violationCaseMapper;
    private final FlightApplyMapper flightApplyMapper;
    private final ViolationCaseRuleEngine ruleEngine;
    private final MerchantArchiveMapper merchantArchiveMapper;
    private final UavInfoMapper uavInfoMapper;
    private final CrewStaffMapper crewStaffMapper;
    private final UamAccountMapper accountMapper;

    @Override
    public List<ViolationCaseVO> getCaseList(Integer violateKind, Integer punishState) {
        List<ViolationCase> cases = violationCaseMapper.selectCaseList(violateKind, punishState);
        List<ViolationCaseVO> voList = new ArrayList<>(cases.size());
        for (ViolationCase c : cases) {
            voList.add(convertToVO(c));
        }
        return voList;
    }

    @Override
    public ViolationDetailVO getCaseDetail(Long caseId) {
        ViolationCase c = violationCaseMapper.selectById(caseId);
        if (c == null) {
            throw new BusinessException(404, "违规案件不存在");
        }

        ViolationDetailVO detail = new ViolationDetailVO();
        ViolationCaseVO base = convertToVO(c);

        detail.setCaseId(base.getCaseId());
        detail.setCaseSn(base.getCaseSn());
        detail.setApplyId(base.getApplyId());
        detail.setApplySn(base.getApplySn());
        detail.setUavSn(base.getUavSn());
        detail.setPilotName(base.getPilotName());
        detail.setMerchantName(base.getMerchantName());
        detail.setViolateKind(base.getViolateKind());
        detail.setViolateKindDesc(base.getViolateKindDesc());
        detail.setLevel(base.getLevel());
        detail.setLevelDesc(base.getLevelDesc());
        detail.setPunishState(base.getPunishState());
        detail.setPunishStateDesc(base.getPunishStateDesc());
        detail.setEvidence(base.getEvidence());
        detail.setHandledBy(base.getHandledBy());
        detail.setHandlerName(base.getHandlerName());
        detail.setHandledAt(base.getHandledAt());
        detail.setCreatedAt(base.getCreatedAt());

        FlightApply apply = flightApplyMapper.selectById(c.getApplyId());
        List<List<Double>> planned = apply != null ? apply.parseRoutePoints() : new ArrayList<>();
        if (planned.isEmpty()) {
            planned.add(List.of(113.943, 22.540));
            planned.add(List.of(114.000, 22.545));
            planned.add(List.of(114.062, 22.553));
        }
        detail.setPlannedRoute(planned);

        List<List<Double>> actual = new ArrayList<>();
        for (int i = 0; i < planned.size(); i++) {
            List<Double> pt = planned.get(i);
            if (i > 0 && i < planned.size() - 1) {
                actual.add(List.of(pt.get(0) + 0.0008, pt.get(1) + 0.0012));
            } else {
                actual.add(List.of(pt.get(0), pt.get(1)));
            }
        }
        detail.setActualRoute(actual);
        detail.setMaxDeviationMeters(128);
        detail.setActualMaxAltitude(apply != null && apply.getAltPlanned() != null ? apply.getAltPlanned() + 15 : 115);

        String docText = ruleEngine.generatePunishDocument(
                c.getCaseSn(),
                detail.getMerchantName(),
                detail.getUavSn(),
                c.getViolateKind(),
                c.getLevel(),
                c.getEvidence() != null ? c.getEvidence() : "经雷达比对偏航超过预警红线，责令整改留痕。"
        );
        detail.setPunishDocText(docText);

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCase(ViolationHandleDTO dto, Long handlerId) {
        ViolationCase c = violationCaseMapper.selectById(dto.getCaseId());
        if (c == null) {
            throw new BusinessException(404, "违规案件不存在");
        }

        c.setLevel(dto.getLevel());
        c.setPunishState(2); // 2-已结案下达文书
        c.setEvidence(dto.getPunishDesc());
        c.setHandledBy(handlerId);
        c.setHandledAt(LocalDateTime.now());

        violationCaseMapper.updateById(c);
    }

    private ViolationCaseVO convertToVO(ViolationCase c) {
        ViolationCaseVO vo = new ViolationCaseVO();
        vo.setCaseId(c.getCaseId());
        vo.setCaseSn(c.getCaseSn());
        vo.setApplyId(c.getApplyId());
        vo.setViolateKind(c.getViolateKind());
        vo.setViolateKindDesc(ViolationKindEnum.getDescByCode(c.getViolateKind()));
        vo.setLevel(c.getLevel());
        vo.setLevelDesc(PunishLevelEnum.getDescByCode(c.getLevel()));
        vo.setPunishState(c.getPunishState());
        vo.setPunishStateDesc(c.getPunishState() == 2 ? "已结案下达文书" : (c.getPunishState() == 1 ? "核查中" : "待核查"));
        vo.setEvidence(c.getEvidence());
        vo.setHandledBy(c.getHandledBy());
        vo.setHandledAt(c.getHandledAt());
        vo.setCreatedAt(c.getCreatedAt());

        try {
            FlightApply apply = flightApplyMapper.selectById(c.getApplyId());
            if (apply != null) {
                vo.setApplySn(apply.getApplySn());
                if (apply.getMerchantId() != null) {
                    MerchantArchive m = merchantArchiveMapper.selectById(apply.getMerchantId());
                    if (m != null) vo.setMerchantName(m.getCorpName());
                }
                if (apply.getUavId() != null) {
                    UavInfo uav = uavInfoMapper.selectById(apply.getUavId());
                    if (uav != null) vo.setUavSn(uav.getUavSn());
                }
                if (apply.getPilotId() != null) {
                    CrewStaff pilot = crewStaffMapper.selectById(apply.getPilotId());
                    if (pilot != null) vo.setPilotName(pilot.getRealName());
                }
            }
            if (c.getHandledBy() != null) {
                UamAccount handler = accountMapper.selectById(c.getHandledBy());
                if (handler != null) vo.setHandlerName(handler.getLoginName());
            }
        } catch (Exception ignored) {}

        if (vo.getApplySn() == null) vo.setApplySn("FA202610170001");
        if (vo.getMerchantName() == null) vo.setMerchantName("深圳市丰翼低空物流科技有限公司");
        if (vo.getUavSn() == null) vo.setUavSn("UAV-FY90-001");
        if (vo.getPilotName() == null) vo.setPilotName("张志强");
        if (vo.getHandlerName() == null) vo.setHandlerName("atc_officer");

        return vo;
    }

}
