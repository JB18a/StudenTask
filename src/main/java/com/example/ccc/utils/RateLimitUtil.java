package com.example.ccc.utils;

import com.example.ccc.common.RedisKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitUtil {

    @Autowired
    private RedisUtil redisUtil;

    public boolean isAllowed(String api, int maxRequests, int windowSeconds, HttpServletRequest request) {
        String ip = getClientIp(request);
        String key = RedisKeys.RATE_LIMIT + ip + ":" + api;

        Long count = redisUtil.increment(key, 1);

        if (count == 1) {
            redisUtil.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        return count <= maxRequests;
    }

    public long getCurrentCount(String api, HttpServletRequest request) {
        String ip = getClientIp(request);
        String key = RedisKeys.RATE_LIMIT + ip + ":" + api;
        Object value = redisUtil.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
