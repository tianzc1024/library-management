package com.library.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存级Token黑名单。
 * 登出时将Token加入黑名单；过滤器在有token的请求中校验黑名单。
 * 定期清理过期条目（由scheduled任务执行）。
 */
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳(System.currentTimeMillis()) */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    /** 清理间隔：60秒 */
    private volatile long lastCleanup = System.currentTimeMillis();
    private static final long CLEANUP_INTERVAL = 60_000L;

    /** 将token加入黑名单，记录其过期时间 */
    public void add(String token, long expirationMillis) {
        blacklist.put(token, expirationMillis);
        cleanupIfNeeded();
    }

    /** 检查token是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        Long expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /** 惰性清理过期条目 */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            lastCleanup = now;
            blacklist.entrySet().removeIf(e -> now > e.getValue());
        }
    }
}
