package com.fly.platform.alarm.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fly.platform.alarm.entity.AlarmEvent;
import com.fly.platform.alarm.enums.AlarmStateEnum;
import com.fly.platform.alarm.vo.AlarmTimelineItemVO;
import com.fly.platform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警事件三态单向流转状态机驱动器
 * 0未处置 -> 1处置中 -> 2已关闭
 * 严格守护单向流转闭环，每次状态跃迁强制追加时间线审计记录
 *
 * @author SoarCloud Team
 */
@Slf4j
@Component
public class AlarmStateMachine {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 驱动告警进入处置中状态 (0 -> 1)
     *
     * @param alarm        告警实体
     * @param operatorName 操作人员姓名
     * @param note         处置方案说明
     */
    public void process(AlarmEvent alarm, String operatorName, String note) {
        if (alarm == null) {
            throw new BusinessException(404, "告警事件不存在");
        }
        Integer currentState = alarm.getState();
        if (currentState == null) {
            currentState = AlarmStateEnum.PENDING.getCode();
        }

        if (!AlarmStateEnum.PENDING.getCode().equals(currentState)) {
            String currentDesc = AlarmStateEnum.getDescByCode(currentState);
            throw new BusinessException(400, String.format("告警当前处于【%s】状态，无法执行开始处置操作", currentDesc));
        }

        alarm.setState(AlarmStateEnum.PROCESSING.getCode());
        alarm.updateAudit();

        appendTimeline(alarm, operatorName, "开始应急处置", note != null && !note.trim().isEmpty() ? note.trim() : "值班人员已接管并启动应急预案");
    }

    /**
     * 驱动告警完成处置并办结关闭 (1 -> 2)
     *
     * @param alarm        告警实体
     * @param operatorName 操作人员姓名
     * @param note         办结说明
     */
    public void close(AlarmEvent alarm, String operatorName, String note) {
        if (alarm == null) {
            throw new BusinessException(404, "告警事件不存在");
        }
        Integer currentState = alarm.getState();
        if (currentState == null) {
            currentState = AlarmStateEnum.PENDING.getCode();
        }

        if (AlarmStateEnum.CLOSED.getCode().equals(currentState)) {
            throw new BusinessException(400, "该告警事件已被办结关闭，无法重复操作");
        }

        // 仅允许从处置中状态关闭（若未处置需先开始处置或直接关闭，此处严格执行 1->2 守卫）
        if (!AlarmStateEnum.PROCESSING.getCode().equals(currentState)) {
            throw new BusinessException(400, "告警当前处于【未处置】状态，须先开始处置接管后再办结关闭");
        }

        alarm.setState(AlarmStateEnum.CLOSED.getCode());
        alarm.setHandledAt(LocalDateTime.now());
        alarm.updateAudit();

        appendTimeline(alarm, operatorName, "办结解除告警", note != null && !note.trim().isEmpty() ? note.trim() : "现场风险已排除，告警正式关闭");
    }

    /**
     * 向告警时间线追加一条处置动作
     */
    public void appendTimeline(AlarmEvent alarm, String operatorName, String action, String note) {
        List<AlarmTimelineItemVO> list = parseTimeline(alarm.getTimeline());
        AlarmTimelineItemVO item = new AlarmTimelineItemVO();
        item.setAt(LocalDateTime.now().format(DATE_FORMATTER));
        item.setBy(operatorName != null ? operatorName : "系统调度中枢");
        item.setAction(action);
        item.setNote(note);
        list.add(item);

        try {
            alarm.setTimeline(OBJECT_MAPPER.writeValueAsString(list));
        } catch (Exception e) {
            log.error("序列化告警时间线失败: {}", e.getMessage());
        }
    }

    /**
     * 解析时间线 JSON 字符串为列表
     */
    public static List<AlarmTimelineItemVO> parseTimeline(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<AlarmTimelineItemVO>>() {});
        } catch (Exception e) {
            log.warn("反序列化告警时间线失败, json={}", json);
            return new ArrayList<>();
        }
    }
}
