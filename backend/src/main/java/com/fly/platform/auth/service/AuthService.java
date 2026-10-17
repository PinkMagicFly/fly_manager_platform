package com.fly.platform.auth.service;

import com.fly.platform.auth.dto.LoginRequestDTO;
import com.fly.platform.auth.dto.LoginResponseDTO;
import com.fly.platform.auth.vo.CurrentUserVO;

/**
 * 账户认证与权限中心业务接口
 *
 * @author SoarCloud Team
 */
public interface AuthService {

    /**
     * 多模式登录认证 (密码登录 / 验证码登录)
     *
     * @param requestDTO 登录请求参数
     * @param clientIp   客户端IP
     * @return 登录结果与Token会话凭证
     */
    LoginResponseDTO login(LoginRequestDTO requestDTO, String clientIp);

    /**
     * 退出当前登录会话
     */
    void logout();

    /**
     * 查询当前登录用户的完整上下文视图 (角色、权限树)
     *
     * @return 当前用户VO
     */
    CurrentUserVO getCurrentUser();

    /**
     * 发送短信验证码 (开发阶段提供Mock打印)
     *
     * @param mobile 目标手机号
     * @return 生成的验证码
     */
    String sendSmsCode(String mobile);

    /**
     * 修改登录密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(String oldPassword, String newPassword);
}
