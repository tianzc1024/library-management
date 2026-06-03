package com.library.controller;

import com.library.common.Result;
import com.library.dto.LoginRequest;
import com.library.dto.LoginResponse;
import com.library.entity.SystemUser;
import com.library.security.JwtUtils;
import com.library.security.RateLimiter;
import com.library.security.TokenBlacklist;
import com.library.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        // 频率限制
        String clientIp = getClientIp(httpRequest);
        if (!rateLimiter.tryAcquire(clientIp)) {
            long seconds = rateLimiter.getLockSeconds(clientIp);
            return Result.error(429, "登录尝试过于频繁，请" + seconds + "秒后重试");
        }

        SystemUser user = systemUserService.findByUsername(request.getUsername());
        if (user == null || user.getStatus() == 0) {
            return Result.error(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getUsername(), null, user.getRole(), "ADMIN");

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        return Result.success(resp);
    }

    @GetMapping("/current-user")
    public Result<LoginResponse> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Result.unauthorized();
        }

        String username = auth.getName();
        SystemUser user = systemUserService.findByUsername(username);
        if (user == null || user.getStatus() == 0) {
            return Result.unauthorized();
        }

        LoginResponse resp = new LoginResponse();
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRole(user.getRole());
        return Result.success(resp);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            String token = (String) details.get("token");
            if (token != null) {
                tokenBlacklist.add(token, jwtUtils.getExpirationFromToken(token).getTime());
            }
        }
        SecurityContextHolder.clearContext();
        return Result.success("退出成功");
    }

    /** 获取客户端真实IP */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}
