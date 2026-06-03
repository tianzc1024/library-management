package com.library.controller;

import com.library.common.Result;
import com.library.entity.User;
import com.library.security.JwtUtils;
import com.library.security.RateLimiter;
import com.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/borrower")
public class BorrowerAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RateLimiter rateLimiter;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> request,
                                              HttpServletRequest httpRequest) {
        // 频率限制
        String clientIp = getClientIp(httpRequest);
        if (!rateLimiter.tryAcquire(clientIp)) {
            long seconds = rateLimiter.getLockSeconds(clientIp);
            return Result.error(429, "登录尝试过于频繁，请" + seconds + "秒后重试");
        }

        String phone = request.get("phone");
        String password = request.get("password");

        if (phone == null || phone.isEmpty() || password == null || password.isEmpty()) {
            return Result.error(400, "手机号和密码不能为空");
        }

        User user = userService.findByPhone(phone);
        if (user == null) {
            return Result.error(401, "手机号或密码错误");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return Result.error(401, "该账号未设置密码，请联系管理员");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "手机号或密码错误");
        }

        String token = jwtUtils.generateToken(phone, user.getId(), "BORROWER", "BORROWER");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("phone", phone);
        data.put("name", user.getName());
        data.put("userId", user.getId());
        data.put("userType", "BORROWER");

        return Result.success(data);
    }

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
