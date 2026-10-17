package com.fly.platform.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.asset.entity.*;
import com.fly.platform.asset.enums.UavStateEnum;
import com.fly.platform.asset.mapper.*;
import com.fly.platform.order.entity.CargoOrder;
import com.fly.platform.order.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能调度推荐算法决策引擎
 *
 * @author SoarCloud Team
 */
@Service
@RequiredArgsConstructor
public class SmartDispatchRecommendService {

    private final UavInfoMapper uavInfoMapper;
    private final CrewStaffMapper crewStaffMapper;
    private final BaseSiteMapper baseSiteMapper;

    public SmartDispatchResultVO recommendDispatch(CargoOrder order) {
        BigDecimal weight = order.getGoodsWeight() != null ? order.getGoodsWeight() : BigDecimal.ZERO;
        double dist = 10.0;
        if (order.getSenderLng() != null && order.getSenderLat() != null
                && order.getReceiverLng() != null && order.getReceiverLat() != null) {
            dist = FeeCalculatorService.calculateDistanceKm(order.getSenderLng(), order.getSenderLat(),
                    order.getReceiverLng(), order.getReceiverLat());
        }
        BigDecimal distanceKm = BigDecimal.valueOf(dist);

        LambdaQueryWrapper<UavInfo> query = new LambdaQueryWrapper<UavInfo>().eq(UavInfo::getIsDeleted, 0);
        if (order.getMerchantId() != null) query.eq(UavInfo::getMerchantId, order.getMerchantId());
        List<UavInfo> allUavs = uavInfoMapper.selectList(query);
        List<SmartDispatchCandidateVO> candidates = evaluateUavs(allUavs, weight, distanceKm);

        SmartDispatchResultVO result = new SmartDispatchResultVO();
        result.setOrderId(order.getOrderId());
        result.setGoodsWeight(weight.doubleValue());
        result.setDistanceKm(dist);
        result.setCandidateUavs(candidates);
        candidates.stream().filter(c -> Boolean.TRUE.equals(c.getIsRecommended())).findFirst().ifPresent(result::setRecommendedUav);

        List<CrewStaff> allCrew = crewStaffMapper.selectAllCrew();
        List<SmartDispatchResultVO.PilotOptionVO> pilots = allCrew.stream()
                .filter(c -> c.getCrewType() != null && c.getCrewType() == 1 && c.getStatus() != null && c.getStatus() == 1)
                .map(c -> {
                    SmartDispatchResultVO.PilotOptionVO opt = new SmartDispatchResultVO.PilotOptionVO();
                    opt.setCrewId(c.getCrewId());
                    opt.setAccountId(c.getAccountId());
                    opt.setRealName(c.getRealName());
                    opt.setLicenseNo(c.getLicenseNo());
                    opt.setFlyHours(c.getFlyHours());
                    opt.setDutyDesc("民用无人机执照有效，今日执勤中");
                    return opt;
                }).collect(Collectors.toList());
        result.setAvailablePilots(pilots);
        return result;
    }

    public List<SmartDispatchCandidateVO> evaluateUavs(List<UavInfo> uavList, BigDecimal weight, BigDecimal distanceKm) {
        if (uavList == null || uavList.isEmpty()) return Collections.emptyList();
        List<SmartDispatchCandidateVO> resultList = new ArrayList<>();

        for (UavInfo uav : uavList) {
            SmartDispatchCandidateVO vo = new SmartDispatchCandidateVO();
            vo.setUavId(uav.getUavId());
            vo.setUavSn(uav.getUavSn());
            vo.setModel(uav.getModel());
            vo.setMaxLoad(uav.getMaxLoad());
            vo.setMaxRangeKm(uav.getMaxRangeKm());
            vo.setBatteryPct(uav.getBatteryPct());
            vo.setHealthScore(uav.getHealthScore());
            vo.setHomeSiteId(uav.getHomeSiteId());
            if (uav.getHomeSiteId() != null && baseSiteMapper != null) {
                try {
                    BaseSite site = baseSiteMapper.selectById(uav.getHomeSiteId());
                    if (site != null) vo.setHomeSiteName(site.getSiteName());
                } catch (Exception ignored) {}
            }

            if (uav.getMaxLoad() == null || uav.getMaxLoad().compareTo(weight) < 0) {
                vo.setScore(0);
                vo.setMatchReason("超重不适配: 最大载重" + uav.getMaxLoad() + "kg < 货物重" + weight + "kg");
                vo.setIsRecommended(false);
                resultList.add(vo);
                continue;
            }
            int battery = uav.getBatteryPct() != null ? uav.getBatteryPct() : 0;
            if (battery < 30) {
                vo.setScore(0);
                vo.setMatchReason("低电量红线拦截: 剩余" + battery + "% < 30%安全线");
                vo.setIsRecommended(false);
                resultList.add(vo);
                continue;
            }
            if (uav.getState() != null && uav.getState() != UavStateEnum.IDLE.getCode()) {
                vo.setScore(0);
                vo.setMatchReason("设备非闲置: " + UavStateEnum.getDescByCode(uav.getState()));
                vo.setIsRecommended(false);
                resultList.add(vo);
                continue;
            }

            int health = uav.getHealthScore() != null ? uav.getHealthScore() : 100;
            double loadRatio = uav.getMaxLoad().compareTo(BigDecimal.ZERO) > 0 ? weight.doubleValue() / uav.getMaxLoad().doubleValue() : 1.0;
            double loadScore = (1.0 - Math.abs(loadRatio - 0.75)) * 20.0;
            int finalScore = (int) Math.round((battery * 0.50) + (health * 0.30) + loadScore);

            vo.setScore(finalScore);
            vo.setMatchReason("综合匹配优异: 载重" + uav.getMaxLoad() + "kg, 电量" + battery + "%, 健康" + health + "分");
            vo.setIsRecommended(false);
            resultList.add(vo);
        }

        resultList.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (!resultList.isEmpty() && resultList.get(0).getScore() > 0) {
            resultList.get(0).setIsRecommended(true);
        }
        return resultList;
    }
}
