package com.fly.platform.sysparam.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fly.platform.admin.entity.AuditLog;
import com.fly.platform.admin.mapper.AuditLogMapper;
import com.fly.platform.common.exception.BusinessException;
import com.fly.platform.common.util.IpUtils;
import com.fly.platform.sysparam.dto.SysParamUpdateDTO;
import com.fly.platform.sysparam.entity.SysParam;
import com.fly.platform.sysparam.mapper.SysParamMapper;
import com.fly.platform.sysparam.service.SysParamService;
import com.fly.platform.sysparam.vo.SysParamVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 平台系统参数配置服务实现
 *
 * @author SoarCloud Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysParamServiceImpl implements SysParamService {

    private final SysParamMapper sysParamMapper;
    private final AuditLogMapper auditLogMapper;

    /** 内存级参数高速缓存字典，实现毫秒级读取与热刷新 */
    private final Map<String, String> paramCache = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String cached = paramCache.get(key);
        if (cached != null) {
            return cached;
        }

        SysParam param = sysParamMapper.selectOne(
                new LambdaQueryWrapper<SysParam>()
                        .eq(SysParam::getParamKey, key)
                        .last("LIMIT 1")
        );
        if (param != null && param.getParamValue() != null) {
            paramCache.put(key, param.getParamValue());
            return param.getParamValue();
        }
        return null;
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(val.trim());
        } catch (Exception e) {
            log.warn("解析参数[{}]为BigDecimal失败: val={}, 回退默认值={}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public Integer getInt(String key, Integer defaultValue) {
        String val = get(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            log.warn("解析参数[{}]为Integer失败: val={}, 回退默认值={}", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void put(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            throw new BusinessException(400, "参数Key不能为空");
        }
        if (value == null) {
            throw new BusinessException(400, "参数值不能为空");
        }

        SysParam param = sysParamMapper.selectOne(
                new LambdaQueryWrapper<SysParam>()
                        .eq(SysParam::getParamKey, key)
                        .last("LIMIT 1")
        );
        if (param == null) {
            throw new BusinessException(404, "系统运行参数不存在: " + key);
        }

        String oldValue = param.getParamValue();
        param.setParamValue(value);
        param.updateAudit();
        sysParamMapper.updateById(param);

        // 更新本地内存缓存
        paramCache.put(key, value);

        // 记录强审计日志流水
        recordAuditLog(key, oldValue, value);
    }

    @Override
    public List<SysParamVO> listAll() {
        List<SysParam> list = sysParamMapper.selectList(
                new LambdaQueryWrapper<SysParam>().orderByAsc(SysParam::getParamId)
        );
        return list.stream().map(p -> {
            SysParamVO vo = new SysParamVO();
            vo.setParamId(p.getParamId());
            vo.setGroupName(p.getGroupName());
            vo.setParamKey(p.getParamKey());
            vo.setParamValue(p.getParamValue());
            vo.setRemark(p.getRemark());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateParams(List<SysParamUpdateDTO> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (SysParamUpdateDTO dto : params) {
            put(dto.getParamKey(), dto.getParamValue());
        }
    }

    @Override
    public void clearCache() {
        paramCache.clear();
    }

    /**
     * 写入参数变更审计日志
     */
    private void recordAuditLog(String key, String oldValue, String newValue) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setModuleName("系统参数");
            auditLog.setActionName("修改运行参数 " + key);
            auditLog.setReqParams("旧值: " + oldValue + " -> 新值: " + newValue);
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLog.setCostTime(0L);
            auditLog.setRespStatus(200);

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                auditLog.setRequestUri(req.getRequestURI());
                auditLog.setClientIp(IpUtils.getClientIp(req));
                String kind = req.getHeader("Client-Kind");
                auditLog.setClientKind(kind != null ? kind : "pc");
            } else {
                auditLog.setRequestUri("/api/v1/params");
                auditLog.setClientIp("127.0.0.1");
                auditLog.setClientKind("system");
            }

            if (StpUtil.isLogin()) {
                auditLog.setAccountId(StpUtil.getLoginIdAsLong());
                Object loginNameObj = StpUtil.getSession().get("loginName");
                auditLog.setLoginName(loginNameObj != null ? loginNameObj.toString() : "user_" + StpUtil.getLoginId());
            } else {
                auditLog.setAccountId(0L);
                auditLog.setLoginName("system");
            }

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("写入参数审计日志失败: {}", e.getMessage(), e);
        }
    }
}
