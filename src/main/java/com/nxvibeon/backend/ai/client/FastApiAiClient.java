package com.nxvibeon.backend.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nxvibeon.backend.ai.dto.AiChatRequest;
import com.nxvibeon.backend.ai.dto.AiChatResponse;
import com.nxvibeon.backend.ai.dto.IndexDocumentRequest;
import com.nxvibeon.backend.ai.dto.IndexDocumentResponse;
import com.nxvibeon.backend.logging.JsonLogMasking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FastApiAiClient {
    private static final Logger log = LoggerFactory.getLogger(FastApiAiClient.class);

    private final WebClient webClient;
    private final FastApiAiProperties properties;
    private final ObjectMapper objectMapper;

    public FastApiAiClient(WebClient fastApiAiWebClient, FastApiAiProperties properties, ObjectMapper objectMapper) {
        this.webClient = fastApiAiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> health() {
        String uri = "/internal/v1/health";
        logOutboundRequest("FASTAPI_HEALTH", uri, null);
        Map<String, Object> response = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .block(Duration.ofMillis(properties.getReadTimeoutMs()));
        logOutboundResponse("FASTAPI_HEALTH", uri, response);
        return response;
    }

    public AiChatResponse ragQuery(AiChatRequest request) {
        String uri = "/internal/v1/rag/query";
        logOutboundRequest("FASTAPI_RAG_QUERY", uri, request);
        AiChatResponse response = webClient.post()
            .uri(uri)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AiChatResponse.class)
            .block(Duration.ofMillis(properties.getReadTimeoutMs()));
        logOutboundResponse("FASTAPI_RAG_QUERY", uri, response);
        return response;
    }

    public IndexDocumentResponse indexDocument(IndexDocumentRequest request) {
        String uri = "/internal/v1/index/documents";
        logOutboundRequest("FASTAPI_INDEX_DOCUMENT", uri, request);
        IndexDocumentResponse response = webClient.post()
            .uri(uri)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(IndexDocumentResponse.class)
            .block(Duration.ofMillis(properties.getReadTimeoutMs()));
        logOutboundResponse("FASTAPI_INDEX_DOCUMENT", uri, response);
        return response;
    }

    private void logOutboundRequest(String action, String uri, Object body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direction", "OUTBOUND_REQUEST");
        payload.put("message", "FastAPI로 넘겨주는 내용");
        payload.put("action", action);
        payload.put("uri", uri);
        payload.put("body", body);
        log.debug("FastAPI로 넘겨주는 내용: {}", JsonLogMasking.toSafeJson(objectMapper, payload));
    }

    private void logOutboundResponse(String action, String uri, Object body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direction", "INBOUND_RESPONSE");
        payload.put("message", "FastAPI에서 받은 내용");
        payload.put("action", action);
        payload.put("uri", uri);
        payload.put("body", body);
        log.debug("FastAPI에서 받은 내용: {}", JsonLogMasking.toSafeJson(objectMapper, payload));
    }
}
