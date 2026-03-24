package com.xdr.asset.client;

import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineServiceClient {

    private final RestTemplate restTemplate;

    @Value("${xdr.services.baseline-url:http://localhost:8083}")
    private String baselineServiceUrl;

    public Map<String, Object> getBaselineStatus(String agentId) {
        String url = String.format("%s/api/v1/baseline/stats/%s", baselineServiceUrl, agentId);
        try {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {
                    });

            if (response.getBody() != null && response.getBody().getCode() == 200) {
                return response.getBody().getData();
            }
        } catch (Exception e) {
            log.error("Failed to fetch baseline status for scoring: {}", agentId);
        }
        return Map.of();
    }
}
