package com.weekendbasket.app.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendbasket.app.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LogManager.getLogger(RateLimitFilter.class);

    // Separate bucket maps — one keyed by IP, one keyed by phone number
    private final ConcurrentHashMap<String, Bucket> ipBuckets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> phoneBuckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Value("${ratelimit.otp.max-requests}")
    private int otpMaxRequests;

    @Value("${ratelimit.otp.window-minutes}")
    private int otpWindowMinutes;

    @Value("${ratelimit.ip.max-requests}")
    private int ipMaxRequests;

    @Value("${ratelimit.ip.window-minutes}")
    private int ipWindowMinutes;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip   = resolveClientIp(request);

        // ── send-otp: phone-number keyed bucket (stricter) ────────────────────
        if (path.contains("/auth/send-otp")) {
            String phone = extractPhoneFromBody(request);
            if (phone != null) {
                Bucket bucket = phoneBuckets.computeIfAbsent(phone, k -> buildBucket(otpMaxRequests, otpWindowMinutes));
                if (!bucket.tryConsume(1)) {
                    log.warn("OTP rate limit exceeded for phone: {}", phone);
                    reject(response, "Too many OTP requests. Please wait " + otpWindowMinutes + " minutes before trying again.");
                    return;
                }
            }
        }

        // ── IP-based limit for all requests ───────────────────────────────────
        Bucket ipBucket = ipBuckets.computeIfAbsent(ip, k -> buildBucket(ipMaxRequests, ipWindowMinutes));
        if (!ipBucket.tryConsume(1)) {
            log.warn("IP rate limit exceeded for: {}", ip);
            reject(response, "Too many requests. Please slow down.");
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket buildBucket(int maxRequests, int windowMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxRequests)
                .refillGreedy(maxRequests, Duration.ofMinutes(windowMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractPhoneFromBody(HttpServletRequest request) {
        try {
            if (request instanceof ContentCachingRequestWrapper wrapper) {
                // Try cached content first (already read)
                byte[] cached = wrapper.getContentAsByteArray();
                String body = cached.length > 0
                        ? new String(cached)
                        : wrapper.getReader().lines().reduce("", String::concat);
                if (body.contains("phoneNumber")) {
                    var node = objectMapper.readTree(body);
                    var phoneNode = node.get("phoneNumber");
                    return phoneNode != null ? phoneNode.asText() : null;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract phone from body for rate limiting: {}", e.getMessage());
        }
        return null;
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = new ErrorResponse(
                429, "Too Many Requests", message, LocalDateTime.now());
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
