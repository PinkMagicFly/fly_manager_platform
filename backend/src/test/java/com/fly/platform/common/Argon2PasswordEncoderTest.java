package com.fly.platform.common;

import com.fly.platform.common.security.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("单元测试: 密码加盐散列与匹配验证")
public class Argon2PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("断言相同明文密码两次哈希值各异(随机加盐)，但均能通过 matches 校验")
    public void testSaltHashMatches() {
        String raw = "admin123456";
        String encoded1 = encoder.encode(raw);
        String encoded2 = encoder.encode(raw);
        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(encoder.matches(raw, encoded1)).isTrue();
        assertThat(encoder.matches(raw, encoded2)).isTrue();
        assertThat(encoder.matches("wrong_pwd", encoded1)).isFalse();
    }

    @Test
    @DisplayName("断言预设 BCrypt 密文能够正常通过种子密码比对")
    public void testPresetSeedPasswordMatches() {
        String seedHash = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";
        assertThat(encoder.matches("123456", seedHash)).isTrue();
        assertThat(encoder.matches("654321", seedHash)).isFalse();
    }
}
