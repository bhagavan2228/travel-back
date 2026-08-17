package com.travelapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.dto.PageResponse;
import com.travelapp.dto.food.RestaurantResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.Restaurant;
import com.travelapp.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.travelapp.service.integration.GooglePlacesImageClient;
import com.travelapp.service.integration.WikipediaImageClient;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
    private static final int PAGE_SIZE = 15;

    private final RestaurantRepository restaurantRepository;
    private final DestinationService destinationService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final GooglePlacesImageClient googlePlacesImageClient;
    private final WikipediaImageClient wikipediaImageClient;

    @org.springframework.beans.factory.annotation.Value("${app.google.places-api-key:}")
    private String googlePlacesApiKey;

    @org.springframework.beans.factory.annotation.Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @org.springframework.beans.factory.annotation.Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiBaseUrl;

    @Transactional
    public PageResponse<RestaurantResponse> getRestaurants(Long destinationId, int page) {
        Destination destination = destinationService.getDestination(destinationId);
        ensureRestaurants(destination, null, null);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Restaurant> result = restaurantRepository.findByDestinationIdOrderByRankOrderAsc(
                destinationId, pageable);

        return toPage(result.map(this::toRestaurantResponse));
    }

    @Transactional
    public PageResponse<RestaurantResponse> searchRestaurantsByCoordinates(Double latitude, Double longitude, int page) {
        List<Restaurant> restaurants = fetchRestaurants(null, latitude, longitude);
        
        List<RestaurantResponse> dtos = restaurants.stream().map(this::toRestaurantResponse).toList();
        
        int start = Math.min(page * PAGE_SIZE, dtos.size());
        int end = Math.min(start + PAGE_SIZE, dtos.size());
        List<RestaurantResponse> pageContent = dtos.subList(start, end);
        
        return PageResponse.<RestaurantResponse>builder()
                .content(pageContent)
                .page(page)
                .size(PAGE_SIZE)
                .totalElements(dtos.size())
                .totalPages((int) Math.ceil((double) dtos.size() / PAGE_SIZE))
                .build();
    }

    private void ensureRestaurants(Destination destination, Double lat, Double lng) {
        if (restaurantRepository.existsByDestinationId(destination.getId())) {
            return;
        }
        
        List<Restaurant> restaurants = fetchRestaurants(destination, lat, lng);
        for (Restaurant r : restaurants) {
            if (r.getGooglePlaceId() != null) {
                Optional<Restaurant> existing = restaurantRepository.findByGooglePlaceId(r.getGooglePlaceId());
                if (existing.isPresent()) continue;
            }
            restaurantRepository.save(r);
        }
    }

    @org.springframework.beans.factory.annotation.Value("${app.grok.api-key:}")
    private String grokApiKey;

    @org.springframework.beans.factory.annotation.Value("${app.grok.base-url:https://api.x.ai/v1}")
    private String grokBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.grok.model:grok-4.3}")
    private String grokModel;

    /**
     * Main dispatcher: tries Google Places first, falls back to Gemini, then Grok.
     */
    private List<Restaurant> fetchRestaurants(Destination destination, Double lat, Double lng) {
        if (googlePlacesApiKey != null && !googlePlacesApiKey.isBlank()) {
            List<Restaurant> results = fetchFromGooglePlaces(destination, lat, lng);
            if (!results.isEmpty()) return results;
        }

        // Fallback to Gemini
        if (geminiApiKey != null && !geminiApiKey.isBlank() && destination != null) {
            log.info("Google Places unavailable, falling back to Gemini for restaurant data for: {}", destination.getName());
            List<Restaurant> results = fetchFromGemini(destination);
            if (!results.isEmpty()) return results;
        }

        // Fallback to Grok
        if (grokApiKey != null && !grokApiKey.isBlank() && destination != null) {
            log.info("Falling back to Grok for restaurant data for: {}", destination.getName());
            List<Restaurant> results = fetchFromGrok(destination);
            if (!results.isEmpty()) return results;
        }

        // Final fallback: Mock Data
        log.warn("All external APIs failed. Returning mock restaurant data.");
        return generateMockRestaurants(destination);
    }

    private List<Restaurant> generateMockRestaurants(Destination destination) {
        List<Restaurant> mockList = new ArrayList<>();
        String city = destination != null && destination.getCity() != null ? destination.getCity() : "Unknown City";
        
        mockList.add(Restaurant.builder()
                .destination(destination)
                .name("The Golden Fork " + city)
                .address("123 Main Street, " + city)
                .cuisine("Local Fusion")
                .rating(4.8)
                .userRatingsTotal(1250)
                .priceLevel("PRICE_LEVEL_MODERATE")
                .businessStatus("OPERATIONAL")
                .imageUrl(wikipediaImageClient.fetchImageForQuery("Local Fusion food"))
                .rankOrder(1)
                .build());

        mockList.add(Restaurant.builder()
                .destination(destination)
                .name("Bistro de " + city)
                .address("456 Oak Avenue, " + city)
                .cuisine("Fine Dining")
                .rating(4.6)
                .userRatingsTotal(890)
                .priceLevel("PRICE_LEVEL_EXPENSIVE")
                .businessStatus("OPERATIONAL")
                .imageUrl(wikipediaImageClient.fetchImageForQuery("Fine Dining food"))
                .rankOrder(2)
                .build());

        mockList.add(Restaurant.builder()
                .destination(destination)
                .name(city + " Spice Market")
                .address("789 Pine Road, " + city)
                .cuisine("Street Food")
                .rating(4.7)
                .userRatingsTotal(3400)
                .priceLevel("PRICE_LEVEL_INEXPENSIVE")
                .businessStatus("OPERATIONAL")
                .imageUrl(wikipediaImageClient.fetchImageForQuery("Street Food"))
                .rankOrder(3)
                .build());

        return mockList;
    }

    // ── Google Places API ──────────────────────────────────────────────

    private List<Restaurant> fetchFromGooglePlaces(Destination destination, Double lat, Double lng) {
        String url = "https://places.googleapis.com/v1/places:searchText";
        String textQuery;
        if (destination != null) {
             textQuery = "top restaurants in " + (destination.getCity() != null ? destination.getCity() : destination.getName());
             if (lat == null && destination.getLatitude() != null) lat = destination.getLatitude();
             if (lng == null && destination.getLongitude() != null) lng = destination.getLongitude();
        } else {
             textQuery = "top restaurants near me"; 
        }

        String requestBody;
        if (lat != null && lng != null) {
            requestBody = String.format("""
                {
                  "textQuery": "%s",
                  "locationBias": {
                    "circle": {
                      "center": {
                        "latitude": %f,
                        "longitude": %f
                      },
                      "radius": 5000.0
                    }
                  }
                }
                """, textQuery, lat, lng);
        } else {
            requestBody = String.format("{\"textQuery\": \"%s\"}", textQuery);
        }

        String fieldMask = "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.primaryTypeDisplayName,places.priceLevel,places.regularOpeningHours,places.websiteUri,places.photos,places.businessStatus";

        try {
            String response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header("X-Goog-Api-Key", googlePlacesApiKey)
                    .header("X-Goog-FieldMask", fieldMask)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGooglePlacesResponse(response, destination);
        } catch (Exception e) {
            log.error("Failed to fetch restaurants from Google Places", e);
            return new ArrayList<>();
        }
    }

    private List<Restaurant> parseGooglePlacesResponse(String response, Destination destination) throws Exception {
        List<Restaurant> list = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response);
        JsonNode places = root.path("places");
        
        if (places.isMissingNode() || !places.isArray()) {
            return list;
        }

        int index = 0;
        for (JsonNode node : places) {
            if (index >= 15) break;

            String googlePlaceId = node.path("id").asText();
            String name = node.path("displayName").path("text").asText();
            String address = node.path("formattedAddress").asText();
            
            Double latitude = null;
            Double longitude = null;
            if (!node.path("location").isMissingNode()) {
                latitude = node.path("location").path("latitude").asDouble();
                longitude = node.path("location").path("longitude").asDouble();
            }
            
            Double rating = node.path("rating").isMissingNode() ? null : node.path("rating").asDouble();
            Integer userRatingsTotal = node.path("userRatingCount").isMissingNode() ? null : node.path("userRatingCount").asInt();
            String cuisine = node.path("primaryTypeDisplayName").path("text").asText();
            String priceLevel = node.path("priceLevel").asText();
            String website = node.path("websiteUri").asText();
            String businessStatus = node.path("businessStatus").asText();
            
            String imageUrl = null;
            if (node.path("photos").isArray() && node.path("photos").size() > 0) {
                String photoName = node.path("photos").get(0).path("name").asText();
                imageUrl = "https://places.googleapis.com/v1/" + photoName + "/media?maxHeightPx=800&key=" + googlePlacesApiKey;
            }
            
            String mapsUri = "https://www.google.com/maps/place/?q=place_id:" + googlePlaceId;

            if (imageUrl == null || imageUrl.isBlank()) {
                imageUrl = googlePlacesImageClient.fetchImageForQuery(name + " " + destination.getCity() + " restaurant");
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = wikipediaImageClient.fetchImageForQuery(cuisine + " food");
                }
            }

            list.add(Restaurant.builder()
                    .destination(destination)
                    .googlePlaceId(googlePlaceId)
                    .name(name)
                    .address(address)
                    .latitude(latitude)
                    .longitude(longitude)
                    .rating(rating)
                    .userRatingsTotal(userRatingsTotal)
                    .cuisine(cuisine)
                    .priceLevel(priceLevel)
                    .website(website)
                    .businessStatus(businessStatus)
                    .imageUrl(imageUrl)
                    .googleMapsUri(mapsUri)
                    .rankOrder(index + 1)
                    .build());
                    
            index++;
        }
        
        return list;
    }

    // ── Gemini Fallback ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Restaurant> fetchFromGemini(Destination destination) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        String country = destination.getCountry() != null ? destination.getCountry() : "India";

        String prompt = "List the top 15 REAL, actually existing, popular restaurants in " + city + ", " + country + ".\n" +
                "These must be real restaurants that genuinely exist — not invented ones.\n" +
                "For each restaurant, provide the following JSON:\n" +
                "{\n" +
                "  \"restaurants\": [\n" +
                "    {\n" +
                "      \"name\": \"Real Restaurant Name\",\n" +
                "      \"cuisine\": \"Indian / Italian / Chinese / etc.\",\n" +
                "      \"rating\": 4.5,\n" +
                "      \"userRatingsTotal\": 1200,\n" +
                "      \"address\": \"Full real address in " + city + "\",\n" +
                "      \"priceLevel\": \"MODERATE\",\n" +
                "      \"website\": \"https://example.com or empty string if unknown\",\n" +
                "      \"businessStatus\": \"OPERATIONAL\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Rules:\n" +
                "- Rating should be a realistic double between 3.5 and 5.0.\n" +
                "- userRatingsTotal should be a realistic integer (100-10000).\n" +
                "- priceLevel should be one of: INEXPENSIVE, MODERATE, EXPENSIVE, VERY_EXPENSIVE.\n" +
                "- Use REAL restaurant names, addresses that actually exist in " + city + ".\n" +
                "- Return ONLY valid raw JSON. No markdown fences.";

        try {
            String apiUrl = geminiBaseUrl + "/gemini-3.6-flash:generateContent?key=" + geminiApiKey;

            var requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                ),
                "generationConfig", Map.of(
                    "responseMimeType", "application/json",
                    "temperature", 0.3
                )
            );

            String response = webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseGeminiRestaurantResponse(response, destination, city);
        } catch (Exception e) {
            log.error("Failed to fetch restaurants from Gemini for {}: {}", city, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Restaurant> parseGeminiRestaurantResponse(String response, Destination destination, String city) {
        List<Restaurant> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            // Navigate Gemini response structure: candidates[0].content.parts[0].text
            String jsonText = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode data = objectMapper.readTree(jsonText);
            JsonNode restaurants = data.path("restaurants");

            if (!restaurants.isArray()) return list;

            int index = 0;
            for (JsonNode node : restaurants) {
                if (index >= 10) break;

                String name = node.path("name").asText("Restaurant in " + city);
                String cuisine = node.path("cuisine").asText("Local");
                Double rating = node.path("rating").asDouble(4.0);
                Integer userRatingsTotal = node.path("userRatingsTotal").asInt(500);
                String address = node.path("address").asText(city);
                String priceLevel = node.path("priceLevel").asText("MODERATE");
                String website = node.path("website").asText("");
                String businessStatus = node.path("businessStatus").asText("OPERATIONAL");

                // Generate a search-based image URL for the cuisine type
                String cuisineLower = cuisine.toLowerCase().replace(" ", "+");
                String imageUrl = "https://source.unsplash.com/800x600/?" + cuisineLower + "+food+restaurant";

                String mapsUri = "https://www.google.com/maps/search/" +
                        java.net.URLEncoder.encode(name + " " + city, java.nio.charset.StandardCharsets.UTF_8);

                list.add(Restaurant.builder()
                        .destination(destination)
                        .googlePlaceId(null) // Not from Google Places
                        .name(name)
                        .address(address)
                        .latitude(destination.getLatitude())
                        .longitude(destination.getLongitude())
                        .rating(rating)
                        .userRatingsTotal(userRatingsTotal)
                        .cuisine(cuisine)
                        .priceLevel("PRICE_LEVEL_" + priceLevel)
                        .website(website.isEmpty() ? null : website)
                        .businessStatus(businessStatus)
                        .imageUrl(imageUrl)
                        .googleMapsUri(mapsUri)
                        .rankOrder(index + 1)
                        .build());

                index++;
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini restaurant response: {}", e.getMessage());
        }
        return list;
    }

    // ── Response mapping ───────────────────────────────────────────────

    private RestaurantResponse toRestaurantResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .destinationId(r.getDestination() != null ? r.getDestination().getId() : null)
                .name(r.getName())
                .googlePlaceId(r.getGooglePlaceId())
                .cuisine(r.getCuisine())
                .rating(r.getRating())
                .userRatingsTotal(r.getUserRatingsTotal())
                .address(r.getAddress())
                .latitude(r.getLatitude())
                .longitude(r.getLongitude())
                .priceLevel(r.getPriceLevel())
                .website(r.getWebsite())
                .googleMapsUri(r.getGoogleMapsUri())
                .businessStatus(r.getBusinessStatus())
                .imageUrl(r.getImageUrl())
                .build();
    }

    private <T> PageResponse<T> toPage(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    // ── Grok Fallback ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Restaurant> fetchFromGrok(Destination destination) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        String country = destination.getCountry() != null ? destination.getCountry() : "India";

        String prompt = "List the top 10 REAL, actually existing, popular restaurants in " + city + ", " + country + ".\n" +
                "These must be real restaurants that genuinely exist — not invented ones.\n" +
                "Return a JSON object with this structure:\n" +
                "{\n" +
                "  \"restaurants\": [\n" +
                "    {\n" +
                "      \"name\": \"Real Restaurant Name\",\n" +
                "      \"cuisine\": \"Indian / Italian / Chinese / etc.\",\n" +
                "      \"rating\": 4.5,\n" +
                "      \"userRatingsTotal\": 1200,\n" +
                "      \"address\": \"Full real address in " + city + "\",\n" +
                "      \"priceLevel\": \"MODERATE\",\n" +
                "      \"website\": \"https://example.com or empty string if unknown\",\n" +
                "      \"businessStatus\": \"OPERATIONAL\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "Rules:\n" +
                "- Rating: realistic double 3.5-5.0\n" +
                "- userRatingsTotal: realistic integer 100-10000\n" +
                "- priceLevel: INEXPENSIVE, MODERATE, EXPENSIVE, or VERY_EXPENSIVE\n" +
                "- Use REAL names and addresses\n" +
                "- Return ONLY valid raw JSON. No markdown.";

        try {
            var body = Map.of(
                    "model", grokModel,
                    "input", List.of(
                            Map.of("role", "system", "content", "You are a local restaurant expert. Return only JSON responses."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "max_tokens", 2000
            );

            var response = webClientBuilder.build()
                    .post()
                    .uri(grokBaseUrl + "/responses")
                    .header("Authorization", "Bearer " + grokApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return new ArrayList<>();

            var output = (List<Map<String, Object>>) response.get("output");
            if (output == null || output.isEmpty()) return new ArrayList<>();

            String jsonText = (String) output.get(0).get("content");
            if (jsonText == null || jsonText.isBlank()) return new ArrayList<>();

            return parseGrokRestaurantResponse(jsonText, destination, city);
        } catch (Exception e) {
            log.error("Failed to fetch restaurants from Grok for {}: {}", city, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Restaurant> parseGrokRestaurantResponse(String jsonText, Destination destination, String city) {
        List<Restaurant> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            JsonNode restaurants = root.path("restaurants");

            if (!restaurants.isArray()) return list;

            int index = 0;
            for (JsonNode node : restaurants) {
                if (index >= 10) break;

                String name = node.path("name").asText("Restaurant in " + city);
                String cuisine = node.path("cuisine").asText("Local");
                Double rating = node.path("rating").asDouble(4.0);
                Integer userRatingsTotal = node.path("userRatingsTotal").asInt(500);
                String address = node.path("address").asText(city);
                String priceLevel = node.path("priceLevel").asText("MODERATE");
                String website = node.path("website").asText("");
                String businessStatus = node.path("businessStatus").asText("OPERATIONAL");

                String cuisineLower = cuisine.toLowerCase().replace(" ", "+");
                String imageUrl = "https://source.unsplash.com/800x600/?" + cuisineLower + "+food+restaurant";

                String mapsUri = "https://www.google.com/maps/search/" +
                        java.net.URLEncoder.encode(name + " " + city, java.nio.charset.StandardCharsets.UTF_8);

                list.add(Restaurant.builder()
                        .destination(destination)
                        .googlePlaceId(null)
                        .name(name)
                        .address(address)
                        .latitude(destination.getLatitude())
                        .longitude(destination.getLongitude())
                        .rating(rating)
                        .userRatingsTotal(userRatingsTotal)
                        .cuisine(cuisine)
                        .priceLevel("PRICE_LEVEL_" + priceLevel)
                        .website(website.isEmpty() ? null : website)
                        .businessStatus(businessStatus)
                        .imageUrl(imageUrl)
                        .googleMapsUri(mapsUri)
                        .rankOrder(index + 1)
                        .build());

                index++;
            }
        } catch (Exception e) {
            log.error("Failed to parse Grok restaurant response: {}", e.getMessage());
        }
        return list;
    }
}
