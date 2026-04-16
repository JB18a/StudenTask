package com.example.ccc.interceptor;

import com.example.ccc.utils.RateLimitUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitUtil rateLimitUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/")) {
            int maxRequests = 100;
            int windowSeconds = 60;

            if (uri.contains("/login") || uri.contains("/register")) {
                maxRequests = 10;
                windowSeconds = 60;
            }

            if (!rateLimitUtil.isAllowed(uri, maxRequests, windowSeconds, request)) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return false;
            }
        }

        return true;
    }
}
