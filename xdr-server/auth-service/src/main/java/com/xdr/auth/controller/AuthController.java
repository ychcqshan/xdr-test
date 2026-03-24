package com.xdr.auth.controller;

import com.xdr.auth.dto.AgentRegisterRequest;
import com.xdr.auth.dto.LoginRequest;
import com.xdr.auth.dto.LoginResponse;
import com.xdr.auth.service.AuthService;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.xdr.auth.model.UserInfo;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** S-AUTH-001: 用户登录 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** S-AUTH-002: 用户登出 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        authService.logout(token);
        return ApiResponse.ok();
    }

    /** S-AUTH-003: Token刷新 */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.refreshToken(body.get("refreshToken")));
    }

    /** S-AUTH-004: Agent注册 */
    @PostMapping("/agent/register")
    public ApiResponse<Map<String, String>> registerAgent(@RequestBody AgentRegisterRequest request) {
        return ApiResponse.ok(authService.registerAgent(request));
    }

    /** S-AUTH-005: 获取当前用户信息 */
    @GetMapping("/users/me")
    public ApiResponse<UserInfo> me(@RequestHeader("Authorization") String authorization) {
        // 简化处理：实际中应从 JWT 解析 userId，这里假设已经过网关鉴权并透传了 Token
        // 开发阶段先通过 token 获取 subject
        String token = authorization.replace("Bearer ", "");
        com.xdr.common.util.JwtUtil jwtUtil = new com.xdr.common.util.JwtUtil("secret-key-1234567890-min-32-chars"); // 仅作演示，实际应注入
        String loginName = jwtUtil.getSubject(token);

        // 此处应通过 loginName 查库或从 claims 获取，为了演示直接返回对应 userId 的 me
        // 实际生产环境应通过 Context 注入
        return ApiResponse.ok(authService.listUsers().stream()
                .filter(u -> u.getLoginName().equals(loginName))
                .findFirst().orElse(null));
    }

    /** S-AUTH-006: 后台用户管理列表 (Admin) */
    @GetMapping("/users")
    public ApiResponse<List<UserInfo>> listUsers() {
        return ApiResponse.ok(authService.listUsers());
    }

    /** S-AUTH-007: 创建后台管理用户 (Admin) */
    @PostMapping("/users")
    public ApiResponse<Void> createUser(@RequestBody UserInfo user) {
        authService.createUser(user);
        return ApiResponse.ok();
    }
}
