package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Iterator;

@Component
@Slf4j
public class WikipediaImageClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String fetchImageForQuery(String query) {
        if (query == null || query.isBlank()) {
            return getUnsplashFallback(query);
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://en.wikipedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("titles", query)
                    .queryParam("prop", "pageimages")
                    .queryParam("format", "json")
                    .queryParam("pithumbsize", "800")
                    .build()
                    .toUriString();

            // Wikipedia requires a User-Agent header
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "VoyagerTravelApp/1.0 (travel-backend; contact@voyager.app)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode pages = root.path("query").path("pages");
                
                Iterator<JsonNode> elements = pages.elements();
                if (elements.hasNext()) {
                    JsonNode page = elements.next();
                    if (page.has("thumbnail")) {
                        String imgUrl = page.path("thumbnail").path("source").asText();
                        if (imgUrl != null && !imgUrl.isBlank()) {
                            log.info("Found Wikipedia image for {}: {}", query, imgUrl);
                            return imgUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch image from Wikipedia for query {}: {}", query, e.getMessage());
        }

        return getUnsplashFallback(query);
    }

    private String getUnsplashFallback(String query) {
        String safeQuery = (query != null ? query : "travel").replace(" ", "+");
        return "https://source.unsplash.com/800x600/?" + safeQuery + "+travel+landmark";
    }
}

