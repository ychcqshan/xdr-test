package com.xdr.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdr.auth.dto.AgentRegisterRequest;
import com.xdr.auth.dto.LoginRequest;
import com.xdr.auth.dto.LoginResponse;
import com.xdr.auth.mapper.UserInfoMapper;
import com.xdr.auth.model.UserInfo;
import com.xdr.common.exception.UnauthorizedException;
import com.xdr.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserInfoMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private JwtUtil jwtUtil;

    private JwtUtil getJwtUtil() {
        if (jwtUtil == null) {
            jwtUtil = new JwtUtil(jwtSecret);
        }
        return jwtUtil;
    }

    /**
     * 用户登录 - S-AUTH-001
     */
    public LoginResponse login(LoginRequest request) {
        UserInfo user = userMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getLoginName, request.getUsername())
                        .eq(UserInfo::getStatus, 1));

        if (user == null) {
            System.err.println("LOGIN DEBUG: user is null!");
            throw new UnauthorizedException("用户名或密码错误");
        }
        System.err.println("LOGIN DEBUG: Found user: " + user.getLoginName());
        System.err.println("LOGIN DEBUG: DB Password: " + user.getPassword());
        System.err.println("LOGIN DEBUG: Req Password: " + request.getPassword());
        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        System.err.println("LOGIN DEBUG: Matches=" + matches);

        if (!matches) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("userId", user.getId());

        String accessToken = getJwtUtil().generateToken(user.getLoginName(), claims);
        String refreshToken = getJwtUtil().generateRefreshToken(user.getLoginName());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getLoginName(),
                user.getRole(),
                7200 // 2小时
        );
    }

    /**
     * 用户登出 - S-AUTH-002: Token加入Redis黑名单
     */
    public void logout(String token) {
        redisTemplate.opsForValue().set(
                "token:blacklist:" + token, "1", 2, TimeUnit.HOURS);
    }

    /**
     * Token刷新 - S-AUTH-003
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!getJwtUtil().isTokenValid(refreshToken)) {
            throw new UnauthorizedException("RefreshToken已过期");
        }

        String username = getJwtUtil().getSubject(refreshToken);
        UserInfo user = userMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getLoginName, username)
                        .eq(UserInfo::getStatus, 1));

        if (user == null) {
            throw new UnauthorizedException("用户不存在或已禁用");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("userId", user.getId());

        String newAccessToken = getJwtUtil().generateToken(user.getLoginName(), claims);
        String newRefreshToken = getJwtUtil().generateRefreshToken(user.getLoginName());

        return new LoginResponse(
                newAccessToken,
                newRefreshToken,
                user.getLoginName(),
                user.getRole(),
                7200);
    }

    /**
     * Agent注册 - S-AUTH-004
     */
    public Map<String, String> registerAgent(AgentRegisterRequest request) {
        String agentId = "AGENT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "agent");
        claims.put("hostname", request.getHostname());
        claims.put("osType", request.getOsType());

        String agentToken = getJwtUtil().generateToken(agentId, claims);

        Map<String, String> result = new HashMap<>();
        result.put("agentId", agentId);
        result.put("token", agentToken);
        return result;
    }

    /**
     * 获取用户信息 - S-AUTH-005
     */
    public UserInfo me(String userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 用户管理列表 - S-AUTH-006 (Admin Only)
     */
    public java.util.List<UserInfo> listUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<UserInfo>().orderByDesc(UserInfo::getCreatedAt));
    }

    /**
     * 创建后台用户 - S-AUTH-007 (Admin Only)
     */
    public void createUser(UserInfo user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
    }

    /**
     * 验证Token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + token));
    }
}
