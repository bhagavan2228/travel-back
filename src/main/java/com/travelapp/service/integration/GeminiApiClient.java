package com.travelapp.service.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GeminiApiClient {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callGemini(String prompt) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/gemini-3.5-flash:generateContent")
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse text from Gemini response", e);
        }
        return "";
    }

    public List<Map<String, String>> extractEventsFromNews(String newsContext, String city) {
        if (newsContext == null || newsContext.isBlank()) {
            return generateFallbacks(city);
        }

        try {
            String prompt = "You are an AI Event Predictor. Based on the following news headlines and descriptions, " +
                    "extract or predict 10 to 15 upcoming public events, festivals, or cultural happenings for " + city + ". " +
                    "Current Date is " + LocalDate.now() + ". " +
                    "Return ONLY a JSON array of objects. Do not wrap in markdown or any other text. " +
                    "Each object MUST have: 'title' (string), 'date' (string in YYYY-MM-DD format, guess future date if needed), 'category' (string like Cultural, Food, Arts), and 'description' (string, max 150 chars). " +
                    "News:\n" + newsContext;

            String jsonText = callGemini(prompt);
            jsonText = jsonText.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(jsonText, new TypeReference<List<Map<String, String>>>() {});

        } catch (Exception e) {
            log.error("Failed to parse events with Gemini API", e);
            return generateFallbacks(city);
        }
    }

    private List<Map<String, String>> generateFallbacks(String city) {
        // Fallback realistic-looking events if API fails or rate limits
        return List.of(
                Map.of("title", city + " Food & Spice Carnival", "date", LocalDate.now().plusDays(2).toString(), "category", "Food", "description", "Taste the best local delicacies and regional specialties cooked by top chefs."),
                Map.of("title", city + " Community Festival Celebration", "date", LocalDate.now().plusDays(3).toString(), "category", "Cultural", "description", "Join the locals in a grand festive gathering with games, stalls, and celebrations."),
                Map.of("title", city + " Heritage Walking Tour", "date", LocalDate.now().plusDays(4).toString(), "category", "Cultural", "description", "Explore the rich history and hidden stories of historical landmarks with expert guides.")
        );
    }
}
