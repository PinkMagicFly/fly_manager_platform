package com.fly.platform.airspace.domain;

import com.fly.platform.airspace.enums.PunishLevelEnum;
import com.fly.platform.airspace.enums.ViolationKindEnum;
import org.springframework.stereotype.Component;

/**
 * 飞行违规案件定责规则引擎
 * 依据违规行为类别、偏差程度与危害系数智能计算匹配行政处罚等级并生成法定行政处罚文书
 *
 * @author SoarCloud Team
 */
@Component
public class ViolationCaseRuleEngine {

    /**
     * 根据违规行为与偏差数值计算建议行政处罚等级
     *
     * @param violateKind 违规类型
     * @param deviation   偏差数值（偏航米数、超高米数或超时分钟数）
     * @return 匹配建议的行政处罚分级枚举
     */
    public PunishLevelEnum calculatePunishLevel(Integer violateKind, Integer deviation) {
        if (violateKind == null) {
            return PunishLevelEnum.WARNING;
        }

        int val = deviation != null ? deviation : 0;

        // 1. 偏航违规
        if (ViolationKindEnum.DEVIATION.getCode().equals(violateKind)) {
            if (val <= 100) {
                return PunishLevelEnum.WARNING;
            } else if (val <= 300) {
                return PunishLevelEnum.FINE;
            } else {
                return PunishLevelEnum.SUSPEND;
            }
        }

        // 2. 非法闯入禁飞区（高危违法）
        if (ViolationKindEnum.INTRUSION.getCode().equals(violateKind)) {
            if (val > 200) {
                return PunishLevelEnum.REVOKE; // 深度入侵吊销资质
            }
            return PunishLevelEnum.SUSPEND; // 停飞整顿
        }

        // 3. 超越法定限高
        if (ViolationKindEnum.OVER_HEIGHT.getCode().equals(violateKind)) {
            if (val <= 50) {
                return PunishLevelEnum.WARNING;
            } else {
                return PunishLevelEnum.FINE;
            }
        }

        // 4. 超时占用空域
        if (ViolationKindEnum.OVER_TIME.getCode().equals(violateKind)) {
            if (val <= 30) {
                return PunishLevelEnum.WARNING;
            } else {
                return PunishLevelEnum.FINE;
            }
        }

        return PunishLevelEnum.WARNING;
    }

    /**
     * 生成标准格式民用无人机违规行政处罚决定书存证文本
     */
    public String generatePunishDocument(String caseSn, String merchantName, String uavSn,
                                         Integer violateKind, Integer level, String punishDesc) {
        String kindDesc = ViolationKindEnum.getDescByCode(violateKind);
        String levelDesc = PunishLevelEnum.getDescByCode(level);

        return String.format(
                "【深圳市低空飞行协同空管监察局·行政处罚决定书】\n" +
                "文书编号：%s-PENALTY\n" +
                "当事人（责任单位）：%s\n" +
                "涉事执飞装备：%s\n" +
                "案由调查认定：该机在执行低空物流任务期间，发生【%s】合规违规行为。\n" +
                "处罚裁量等级：【%s】\n" +
                "处罚处置说明：%s\n" +
                "依据《中华人民共和国民用航空法》及《无人驾驶航空器飞行管理暂行条例》相关法规，本决定自下发之日起即时生效并归入低空运营诚信档案。\n" +
                "签发机关：民航深圳空管综合监察支队",
                caseSn, merchantName, uavSn, kindDesc, levelDesc, punishDesc
        );
    }
}
