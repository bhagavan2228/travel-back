package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
            return getDefaultImage();
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

            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode pages = root.path("query").path("pages");
                
                Iterator<JsonNode> elements = pages.elements();
                if (elements.hasNext()) {
                    JsonNode page = elements.next();
                    if (page.has("thumbnail")) {
                        return page.path("thumbnail").path("source").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch image from Wikipedia for query {}: {}", query, e.getMessage());
        }

        // If Wikipedia fails or doesn't have an image, we try a fallback search or return default
        return getDefaultImage();
    }

    private String getDefaultImage() {
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c8/Taj_Mahal_in_March_2004.jpg/800px-Taj_Mahal_in_March_2004.jpg";
    }
}
