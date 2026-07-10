package com.algorythm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate limits the endpoints that matter: the auth endpoints (login/register) and
 * any write request under /api. Limits are per caller IP. When a caller exceeds a
 * limit they get a clear 429 with a Retry-After header rather than an error.
 * Disabled in tests via app.ratelimit.enabled=false.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter limiter;
    private final boolean enabled;
    private final int authMax;
    private final int writeMax;
    private final long windowMs;

    public RateLimitInterceptor(
            RateLimiter limiter,
            @Value("${app.ratelimit.enabled:true}") boolean enabled,
            @Value("${app.ratelimit.auth-max:10}") int authMax,
            @Value("${app.ratelimit.write-max:60}") int writeMax,
            @Value("${app.ratelimit.window-seconds:60}") long windowSeconds) {
        this.limiter = limiter;
        this.enabled = enabled;
        this.authMax = authMax;
        this.writeMax = writeMax;
        this.windowMs = windowSeconds * 1000L;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled) {
            return true;
        }
        String ip = clientIp(request);
        if (isAuth(request.getRequestURI())) {
            enforce(response, "auth:" + ip, authMax);
        } else if (isWrite(request)) {
            enforce(response, "write:" + ip, writeMax);
        }
        return true;
    }

    private void enforce(HttpServletResponse response, String key, int max) {
        if (!limiter.allow(key, max, windowMs)) {
            response.setHeader("Retry-After", String.valueOf(windowMs / 1000L));
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests — please slow down and try again shortly");
        }
    }

    private static boolean isAuth(String path) {
        return "/api/auth/login".equals(path) || "/api/auth/register".equals(path);
    }

    private static boolean isWrite(HttpServletRequest request) {
        String method = request.getMethod();
        boolean mutating = "POST".equals(method)
                || "PUT".equals(method)
                || "PATCH".equals(method)
                || "DELETE".equals(method);
        return mutating && request.getRequestURI().startsWith("/api/");
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
