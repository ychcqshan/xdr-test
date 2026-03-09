package com.xdr.baseline.client;

import com.xdr.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetServiceClient {

    private final RestTemplate restTemplate;

    @Value("${xdr.services.asset-url:http://localhost:8082}")
    private String assetServiceUrl;

    public List<Map<String, Object>> getAssetHistory(String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        String url = String.format("%s/api/v1/assets/%s/history?startTime=%s&endTime=%s",
                assetServiceUrl,
                agentId,
                startTime.format(DateTimeFormatter.ISO_DATE_TIME),
                endTime.format(DateTimeFormatter.ISO_DATE_TIME));

        try {
            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {
                    });

            if (response != null && response.getBody() != null && response.getBody().getCode() == 200) {
                List<Map<String, Object>> data = response.getBody().getData();
                return data != null ? data : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to fetch asset history from asset-service", e);
        }
        return List.of();
    }
}
