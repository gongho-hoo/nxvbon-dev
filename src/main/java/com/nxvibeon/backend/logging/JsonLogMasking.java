package com.nxvibeon.backend.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class JsonLogMasking {
    private static final int DEFAULT_MAX_LENGTH = 4000;
    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
        "(?i)(\\\"(?:password|passwd|pwd|secret|token|apiKey|api_key|accessToken|access_token|refreshToken|refresh_token|authorization|cookie)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")"
    );
    private static final Pattern HEADER_SECRET_PATTERN = Pattern.compile(
        "(?i)((?:authorization|cookie|set-cookie|x-api-key)=)[^,}\\]]+"
    );

    private JsonLogMasking() {
    }

    public static String toSafeJson(ObjectMapper objectMapper, Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return sanitize(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            return sanitize(String.valueOf(value));
        }
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String masked = JSON_SECRET_PATTERN.matcher(value).replaceAll("$1***MASKED***$2");
        masked = HEADER_SECRET_PATTERN.matcher(masked).replaceAll("$1***MASKED***");
        return truncate(masked, DEFAULT_MAX_LENGTH);
    }

    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        headers.replaceAll((key, value) -> isSensitiveKey(key) ? "***MASKED***" : truncate(value, 500));
        return headers;
    }

    private static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return lowerKey.contains("authorization")
            || lowerKey.contains("cookie")
            || lowerKey.contains("token")
            || lowerKey.contains("secret")
            || lowerKey.contains("password")
            || lowerKey.contains("api-key")
            || lowerKey.contains("apikey");
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "... [TRUNCATED length=" + value.length() + "]";
    }
}
