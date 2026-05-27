package com.nxvibeon.backend.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpPayloadLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(HttpPayloadLoggingFilter.class);
    private static final int MAX_BODY_LOG_LENGTH = 50000;

    private final ObjectMapper objectMapper;

    public HttpPayloadLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String traceId = getOrCreateTraceId(request);
        MDC.put("traceId", traceId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long startedAt = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long elapsedMs = System.currentTimeMillis() - startedAt;
            logRequest(wrappedRequest, traceId);
            logResponse(wrappedRequest, wrappedResponse, traceId, elapsedMs);

            // ContentCachingResponseWrapper는 응답 본문을 캐시에 보관하므로 반드시 원 응답으로 복사해야 합니다.
            wrappedResponse.copyBodyToResponse();
            MDC.remove("traceId");
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String traceId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", traceId);
        payload.put("direction", "INBOUND_REQUEST");
        payload.put("message", "요청 받은 내용");
        payload.put("method", request.getMethod());
        payload.put("uri", request.getRequestURI());
        payload.put("queryString", request.getQueryString());
        payload.put("clientIp", request.getRemoteAddr());
        payload.put("contentType", request.getContentType());
        payload.put("headers", JsonLogMasking.sanitizeHeaders(extractHeaders(request)));
        payload.put("body", extractRequestBodyForLog(request));
        log.debug("HTTP 요청 받은 내용: {}", JsonLogMasking.toSafeJson(objectMapper, payload));
    }

    private void logResponse(
        ContentCachingRequestWrapper request,
        ContentCachingResponseWrapper response,
        String traceId,
        long elapsedMs
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", traceId);
        payload.put("direction", "OUTBOUND_RESPONSE");
        payload.put("message", "응답으로 넘겨주는 내용");
        payload.put("method", request.getMethod());
        payload.put("uri", request.getRequestURI());
        payload.put("status", response.getStatus());
        payload.put("elapsedMs", elapsedMs);
        payload.put("contentType", response.getContentType());
        payload.put("contentEncoding", response.getHeader("Content-Encoding"));
        payload.put("headers", JsonLogMasking.sanitizeHeaders(extractHeaders(response)));
        payload.put("body", extractResponseBodyForLog(response));
        log.debug("HTTP 응답으로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, payload));
    }

    private String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    private Map<String, String> extractHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, response.getHeader(name));
        }
        return headers;
    }

    private String extractRequestBodyForLog(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        if (!isLoggableContentType(request.getContentType())) {
            return "[SKIPPED_NON_TEXT_CONTENT_TYPE]";
        }
        return sanitizeAndTruncate(new String(content, StandardCharsets.UTF_8));
    }

    private String extractResponseBodyForLog(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }
        if (isCompressed(response)) {
            return "[SKIPPED_COMPRESSED_BODY]";
        }
        if (!isLoggableContentType(response.getContentType())) {
            return "[SKIPPED_NON_TEXT_CONTENT_TYPE]";
        }
        return sanitizeAndTruncate(new String(content, StandardCharsets.UTF_8));
    }

    private boolean isCompressed(HttpServletResponse response) {
        String encoding = response.getHeader("Content-Encoding");
        return encoding != null && !encoding.isBlank();
    }

    private boolean isLoggableContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.contains("application/json")
            || lower.contains("application/problem+json")
            || lower.contains("+json")
            || lower.contains("text/plain")
            || lower.contains("text/event-stream");
    }

    private String sanitizeAndTruncate(String value) {
        String sanitized = JsonLogMasking.sanitize(value);
        if (sanitized == null || sanitized.length() <= MAX_BODY_LOG_LENGTH) {
            return sanitized;
        }
        // UTF-8 바이트 기준이 아니라 Java 문자열 기준으로 잘라 한글 깨짐을 방지합니다.
        return sanitized.substring(0, MAX_BODY_LOG_LENGTH) + "... [TRUNCATED length=" + sanitized.length() + "]";
    }
}
