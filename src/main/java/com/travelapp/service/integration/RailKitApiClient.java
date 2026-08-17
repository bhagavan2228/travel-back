package com.travelapp.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class RailKitApiClient {

    private final WebClient webClient;
    private final String proxyUrl;

    public RailKitApiClient(@Value("${app.railkit.proxy-url:http://localhost:3001}") String proxyUrl) {
        this.proxyUrl = proxyUrl;
        this.webClient = WebClient.builder()
                .baseUrl(proxyUrl)
                .build();
    }

    public boolean isConfigured() {
        return proxyUrl != null && !proxyUrl.isEmpty();
    }

    public String searchTrains(String from, String to, String date) {
        try {
            String url = UriComponentsBuilder.fromPath("/search")
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .queryParam("date", date)
                    .toUriString();

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RailKit proxy error: {}", e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Failed to connect to RailKit proxy at {}: {}", proxyUrl, e.getMessage());
            return null;
        }
    }
}
