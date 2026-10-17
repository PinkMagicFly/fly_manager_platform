package com.fly.platform.common;

import com.fly.platform.common.api.Result;
import com.fly.platform.common.api.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("单元测试: 统一响应包装器测试")
public class ResultResponseTest {

    @Test
    @DisplayName("断言成功出参包含 200 码与正确时间戳")
    public void testSuccessResult() {
        Result<String> res = Result.ok("测试载荷数据");
        assertThat(res.getCode()).isEqualTo(200);
        assertThat(res.getData()).isEqualTo("测试载荷数据");
        assertThat(res.getTimestamp()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("断言失败包装器返回指定错误码")
    public void testFailResult() {
        Result<Void> res = Result.fail(ResultCode.BAD_REQUEST);
        assertThat(res.getCode()).isEqualTo(400);
        assertThat(res.getMessage()).contains("参数错误");
    }
}
