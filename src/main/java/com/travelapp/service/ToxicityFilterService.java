package com.travelapp.service;

import com.travelapp.config.AppProperties;
import com.travelapp.dto.toxicity.ToxicityRequest;
import com.travelapp.dto.toxicity.ToxicityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToxicityFilterService {

    private static final Set<String> TOXIC_KEYWORDS = Set.of(
            "hate", "kill", "stupid", "idiot", "spam", "scam", "fraud", "abuse", "harass"
    );

    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;

    public ToxicityResponse check(ToxicityRequest request) {
        ToxicityResult result = check(request.getText());
        return ToxicityResponse.builder()
                .toxic(result.toxic())
                .score(result.score())
                .flaggedCategories(result.categories())
                .message(result.message())
                .build();
    }

    public ToxicityResult check(String text) {
        if (text == null || text.isBlank()) {
            return new ToxicityResult(false, 0.0, List.of(), "Empty text");
        }
        
        String apiKey = appProperties.getToxicity().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return callPerspectiveApi(text, apiKey);
            } catch (Exception e) {
                log.error("Perspective API call failed, falling back to mock: {}", e.getMessage());
            }
        }
        return getMockedToxicityResult(text);
    }

    @SuppressWarnings("unchecked")
    private ToxicityResult callPerspectiveApi(String text, String apiKey) {
        String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey;
        
        var body = Map.of(
            "comment", Map.of("text", text),
            "languages", List.of("en"),
            "requestedAttributes", Map.of("TOXICITY", Map.of())
        );
        
        var response = webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
                
        if (response == null) {
            throw new IllegalStateException("Empty response from Perspective API");
        }
        
        var attributeScores = (Map<String, Object>) response.get("attributeScores");
        var toxicityScoreMap = (Map<String, Object>) attributeScores.get("TOXICITY");
        var summaryScore = (Map<String, Object>) toxicityScoreMap.get("summaryScore");
        
        double score = ((Number) summaryScore.get("value")).doubleValue();
        
        boolean isToxic = score >= appProperties.getToxicity().getThreshold();
        List<String> flagged = new ArrayList<>();
        if (isToxic) flagged.add("TOXICITY");
        
        return new ToxicityResult(
                isToxic,
                score,
                flagged,
                isToxic ? "Content flagged by Perspective API" : "Content appears safe"
        );
    }

    private ToxicityResult getMockedToxicityResult(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> flagged = new ArrayList<>();
        double score = 0.0;

        for (String keyword : TOXIC_KEYWORDS) {
            if (lower.contains(keyword)) {
                flagged.add(keyword);
                score += 0.40; // bump mock score to test the thresholds
            }
        }

        if (text.length() > 500 && text.chars().filter(c -> c == '!').count() > 5) {
            flagged.add("excessive_punctuation");
            score += 0.20;
        }

        score = Math.min(1.0, score);
        boolean toxic = score >= appProperties.getToxicity().getThreshold();

        return new ToxicityResult(
                toxic,
                score,
                flagged,
                toxic ? "Content may violate community guidelines" : "Content appears safe"
        );
    }

    public record ToxicityResult(boolean toxic, double score, List<String> categories, String message) {}
}
