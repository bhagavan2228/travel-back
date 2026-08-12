package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.enums.FoodSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class GeminiFoodProvider implements ThirdPartyFoodProvider {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final WikipediaImageClient wikipediaImageClient;

    @Override
    public List<FoodRecommendation> fetchTopRestaurants(Destination destination) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        log.info("Fetching real restaurant recommendations for {} using Gemini AI...", city);

        String prompt = "You are a local food expert in " + city + ". Provide a JSON array of the top 5 must-visit local restaurants or street food spots. " +
                "Only return valid JSON array. Each object should have: " +
                "\"name\" (string), \"cuisine\" (string), \"description\" (string, very brief), \"rating\" (number, e.g. 4.5), \"priceRange\" (string, $, $$, or $$$). " +
                "Do not include markdown blocks like ```json.";

        try {
            String jsonResponse = geminiApiClient.callGemini(prompt);
            
            // Clean up possible markdown artifacts if the model includes them despite instructions
            jsonResponse = jsonResponse.replace("```json", "").replace("```", "").trim();

            JsonNode root = objectMapper.readTree(jsonResponse);
            List<FoodRecommendation> recommendations = new ArrayList<>();

            if (root.isArray()) {
                for (JsonNode node : root) {
                    String cuisine = node.path("cuisine").asText("Local");
                    String imageUrl = wikipediaImageClient.fetchImageForQuery(cuisine + " food");
                    if (imageUrl == null || imageUrl.contains("Taj_Mahal")) {
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6d/Good_Food_Display_-_NCI_Visuals_Online.jpg/800px-Good_Food_Display_-_NCI_Visuals_Online.jpg";
                    }

                    recommendations.add(FoodRecommendation.builder()
                            .destination(destination)
                            .name(node.path("name").asText("Unknown Place"))
                            .cuisine(cuisine)
                            .description(node.path("description").asText("A great place to eat."))
                            .rating(node.path("rating").asDouble(4.0))
                            .priceRange(node.path("priceRange").asText("$$"))
                            .address(city) // Gemini might not give exact street address easily, use city
                            .imageUrl(imageUrl)
                            .source(FoodSource.AI_RECOMMENDED)
                            .build());
                }
            }
            
            return recommendations;
            
        } catch (Exception e) {
            log.error("Failed to get food recommendations from Gemini for {}: {}", city, e.getMessage());
            return List.of();
        }
    }
}
