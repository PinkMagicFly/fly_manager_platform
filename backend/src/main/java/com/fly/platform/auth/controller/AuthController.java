package com.fly.platform.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fly.platform.auth.dto.LoginRequestDTO;
import com.fly.platform.auth.dto.LoginResponseDTO;
import com.fly.platform.auth.service.AuthService;
import com.fly.platform.auth.vo.CurrentUserVO;
import com.fly.platform.common.api.Result;
import com.fly.platform.common.audit.Audit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 统一账户认证与多端会话控制器
 *
 * @author SoarCloud Team
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Audit(module = "账户认证", action = "用户登录")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        LoginResponseDTO responseDTO = authService.login(requestDTO, clientIp);
        return Result.ok("登录成功", responseDTO);
    }

    @PostMapping("/logout")
    @Audit(module = "账户认证", action = "退出登录")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok("已安全退出登录", null);
    }

    @GetMapping("/current-user")
    @SaCheckLogin
    public Result<CurrentUserVO> getCurrentUser() {
        return Result.ok(authService.getCurrentUser());
    }

    @PostMapping("/send-sms-code")
    public Result<String> sendSmsCode(@RequestParam String mobile) {
        String code = authService.sendSmsCode(mobile);
        return Result.ok("验证码发送成功（开发环境自动回传）", code);
    }

    @PostMapping("/change-password")
    @SaCheckLogin
    @Audit(module = "账户认证", action = "修改登录密码")
    public Result<Void> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        authService.changePassword(oldPassword, newPassword);
        return Result.ok("密码修改成功，请使用新密码重新登录", null);
    }
}

