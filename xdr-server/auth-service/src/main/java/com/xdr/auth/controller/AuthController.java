package com.xdr.auth.controller;

import com.xdr.auth.dto.AgentRegisterRequest;
import com.xdr.auth.dto.LoginRequest;
import com.xdr.auth.dto.LoginResponse;
import com.xdr.auth.service.AuthService;
import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
