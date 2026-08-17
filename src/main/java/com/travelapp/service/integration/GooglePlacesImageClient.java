package com.travelapp.service.integration;

import com.travelapp.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesImageClient {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchImageForQuery(String query) {
        try {
            String googleApiKey = appProperties.getGoogle().getPlacesApiKey();
            if (googleApiKey == null || googleApiKey.isBlank()) {
                log.warn("No Google API key for places image search, skipping");
                return null;
            }

            String searchUrl = "https://places.googleapis.com/v1/places:searchText";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Goog-Api-Key", googleApiKey);
            headers.set("X-Goog-FieldMask", "places.photos");
            headers.set("Content-Type", "application/json");

            String body = "{\"textQuery\": \"" + query + " landmark\"}";
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(searchUrl, entity, Map.class);
            
            if (response != null && response.containsKey("places")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> places = (List<Map<String, Object>>) response.get("places");
                if (places != null && !places.isEmpty()) {
                    for (Map<String, Object> place : places) {
                        if (place.containsKey("photos")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> photos = (List<Map<String, Object>>) place.get("photos");
                            if (photos != null && !photos.isEmpty()) {
                                String photoName = (String) photos.get(0).get("name");
                                
                                // 2. Construct the photo URL
                                String photoUrl = "https://places.googleapis.com/v1/" + photoName + "/media?maxHeightPx=800&maxWidthPx=800&key=" + googleApiKey;
                                log.info("Fetched Google Places image for query: {}", query);
                                return photoUrl;
                            }
                        }
                    }
                }
            }
            log.warn("No photos found in Google Places API for query: {}", query);
        } catch (Exception e) {
            log.error("Failed to fetch image from Google Places API for query {}: {}", query, e.getMessage());
        }
        return null;
    }
}
