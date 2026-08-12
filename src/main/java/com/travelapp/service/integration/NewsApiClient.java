package com.travelapp.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@Component
@Slf4j
public class NewsApiClient {

    @Value("${app.news.api-key}")
    private String apiKey;

    @Value("${app.news.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchNewsForDestination(String city) {
        try {
            // Looking for recent events and news
            String query = city + " events festivals culture";
            LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
            
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .pathSegment("everything")
                    .queryParam("q", query)
                    .queryParam("from", oneMonthAgo.toString())
                    .queryParam("sortBy", "relevancy")
                    .queryParam("language", "en")
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && "ok".equals(response.get("status"))) {
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                
                // Extract just the titles and descriptions to save Gemini context window
                return articles.stream()
                        .limit(10) // Take top 10 articles
                        .map(a -> a.get("title") + " - " + a.get("description"))
                        .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("Failed to fetch news from NewsAPI for {}", city, e);
        }
        return "";
    }
}
