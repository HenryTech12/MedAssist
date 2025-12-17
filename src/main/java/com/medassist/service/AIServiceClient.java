package com.medassist.service;

import com.medassist.dto.AIServiceRequest;
import com.medassist.dto.AIServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceClient.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    public AIServiceResponse processMessage(AIServiceRequest request) {
        try {
            String url = aiServiceBaseUrl + "/api/v1/message/process";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AIServiceRequest> entity = new HttpEntity<>(request, headers);

            logger.info("Calling AI Service for message: {}", request.getMessageId());

            ResponseEntity<AIServiceResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    AIServiceResponse.class
            );

            AIServiceResponse aiResponse = response.getBody();
            logger.info("AI Service response - Intent: {}, Triage: {}",
                    aiResponse.getIntent(), aiResponse.getTriageLevel());

            return aiResponse;

        } catch (Exception e) {
            logger.error("Failed to call AI Service: {}", e.getMessage());
            throw new RuntimeException("AI Service call failed: " + e.getMessage());
        }
    }
}
