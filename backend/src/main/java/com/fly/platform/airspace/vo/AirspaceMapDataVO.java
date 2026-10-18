package com.fly.platform.airspace.vo;

import com.fly.platform.asset.entity.BaseSite;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 低空空域全景态势聚合数据 VO
 * 单次请求聚合：三区管制网格、起降场站、在飞无人机瞬时轨迹与近24h已批合规航线
 *
 * @author SoarCloud Team
 */
@Data
public class AirspaceMapDataVO {

    /** 三区管制网格区块列表 */
    private List<AirspaceBlockVO> blocks = new ArrayList<>();

    /** 平台起降场站台账列表 */
    private List<BaseSite> sites = new ArrayList<>();

    /** 实时在飞无人机与最新动态列表 */
    private List<FlyingUavVO> flyingUavs = new ArrayList<>();

    /** 近24小时已核准/执行中飞行航线列表 */
    private List<ApprovedRouteVO> approvedRoutes = new ArrayList<>();
}
