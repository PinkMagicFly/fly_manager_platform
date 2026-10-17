package com.fly.platform.airspace;

import com.fly.platform.airspace.domain.ViolationCaseRuleEngine;
import com.fly.platform.airspace.enums.PunishLevelEnum;
import com.fly.platform.airspace.enums.ViolationKindEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 飞行违规案件定责与行政处罚规则引擎测试
 *
 * @author SoarCloud Team
 */
public class ViolationCaseRuleTest {

    private final ViolationCaseRuleEngine ruleEngine = new ViolationCaseRuleEngine();

    @Test
    @DisplayName("测试1：偏航违规阶梯定责（<=100m警告，101~300m罚款，>300m停飞整顿）")
    public void testDeviationPunishLadder() {
        // 轻微偏航 50 米
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.DEVIATION.getCode(), 50))
                .isEqualTo(PunishLevelEnum.WARNING);

        // 中度偏航 150 米
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.DEVIATION.getCode(), 150))
                .isEqualTo(PunishLevelEnum.FINE);

        // 严重偏航 400 米
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.DEVIATION.getCode(), 400))
                .isEqualTo(PunishLevelEnum.SUSPEND);
    }

    @Test
    @DisplayName("测试2：非法闯入禁飞区定责（常规停飞整顿，深度入侵吊销资质）")
    public void testIntrusionPunish() {
        // 入侵 100 米
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.INTRUSION.getCode(), 100))
                .isEqualTo(PunishLevelEnum.SUSPEND);

        // 深度入侵 300 米
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.INTRUSION.getCode(), 300))
                .isEqualTo(PunishLevelEnum.REVOKE);
    }

    @Test
    @DisplayName("测试3：超越法定限高定责（<=50m警告，>50m罚款）")
    public void testOverHeightPunish() {
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.OVER_HEIGHT.getCode(), 20))
                .isEqualTo(PunishLevelEnum.WARNING);

        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.OVER_HEIGHT.getCode(), 80))
                .isEqualTo(PunishLevelEnum.FINE);
    }

    @Test
    @DisplayName("测试4：超时占用空域定责（<=30min警告，>30min罚款）")
    public void testOverTimePunish() {
        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.OVER_TIME.getCode(), 15))
                .isEqualTo(PunishLevelEnum.WARNING);

        assertThat(ruleEngine.calculatePunishLevel(ViolationKindEnum.OVER_TIME.getCode(), 45))
                .isEqualTo(PunishLevelEnum.FINE);
    }

    @Test
    @DisplayName("测试5：行政处罚文书生成内容格式规范性验证")
    public void testGeneratePunishDocument() {
        String doc = ruleEngine.generatePunishDocument(
                "VC202610170001",
                "深圳市丰翼低空物流科技有限公司",
                "UAV-FY90-001",
                ViolationKindEnum.DEVIATION.getCode(),
                PunishLevelEnum.FINE.getCode(),
                "偏离规划航道超120米，责令企业整改并缴纳行政处罚金。"
        );

        assertThat(doc).contains("深圳市低空飞行协同空管监察局·行政处罚决定书");
        assertThat(doc).contains("VC202610170001-PENALTY");
        assertThat(doc).contains("航线严重偏航");
        assertThat(doc).contains("企业罚款");
        assertThat(doc).contains("中华人民共和国民用航空法");
    }
}
