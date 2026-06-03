package com.library.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存级登录频率限制。
 * 同一IP在60秒内最多允许5次登录尝试，超过后锁定60秒。
 */
@Component
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000L;
    private static final long LOCK_MS = 60_000L;

    /** IP -> { attempts, windowStart, lockUntil } 数据 */
    private static class Entry {
        int attempts;
        long windowStart;
        long lockUntil;

        Entry() {
            this.attempts = 1;
            this.windowStart = System.currentTimeMillis();
            this.lockUntil = 0;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * 检查是否允许该IP尝试登录。
     * @return true = 允许, false = 被限流
     */
    public boolean tryAcquire(String clientIp) {
        Entry entry = store.compute(clientIp, (k, v) -> {
            long now = System.currentTimeMillis();
            if (v == null) {
                return new Entry();
            }
            // 如果在锁定期内
            if (v.lockUntil > 0 && now < v.lockUntil) {
                return v; // 保持锁定
            }
            // 过了锁定时间，重置
            if (v.lockUntil > 0 && now >= v.lockUntil) {
                return new Entry();
            }
            // 检查时间窗口
            if (now - v.windowStart > WINDOW_MS) {
                v.attempts = 1;
                v.windowStart = now;
            } else {
                v.attempts++;
            }
            // 超过最大尝试次数则锁定
            if (v.attempts > MAX_ATTEMPTS) {
                v.lockUntil = now + LOCK_MS;
            }
            return v;
        });

        long now = System.currentTimeMillis();
        return entry.lockUntil == 0 || now >= entry.lockUntil;
    }

    /** 获取锁定剩余秒数(用于提示) */
    public long getLockSeconds(String clientIp) {
        Entry entry = store.get(clientIp);
        if (entry == null || entry.lockUntil == 0) return 0;
        long remaining = (entry.lockUntil - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    /** 定期清理过期条目由调用方触发，或依赖entry的惰性重置 */
    public void cleanup() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> {
            Entry v = e.getValue();
            return (v.lockUntil > 0 && now > v.lockUntil) ||
                   (now - v.windowStart > WINDOW_MS + LOCK_MS);
        });
    }
}
