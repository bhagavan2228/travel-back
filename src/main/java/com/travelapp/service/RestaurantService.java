package com.travelapp.service;

import com.travelapp.catalog.RestaurantCatalogGenerator;
import com.travelapp.dto.PageResponse;
import com.travelapp.dto.food.MenuItemResponse;
import com.travelapp.dto.food.RestaurantResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.MenuItem;
import com.travelapp.entity.Restaurant;
import com.travelapp.exception.ApiException;
import com.travelapp.repository.MenuItemRepository;
import com.travelapp.repository.RestaurantRepository;
import com.travelapp.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final DestinationService destinationService;
    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public PageResponse<RestaurantResponse> getRestaurants(Long destinationId, int page) {
        Destination destination = destinationService.getDestination(destinationId);
        ensureRestaurants(destination);

        Pageable pageable = PageRequest.of(page, RestaurantCatalogGenerator.PAGE_SIZE);
        Page<Restaurant> result = restaurantRepository.findByDestinationIdOrderByRankOrderAsc(
                destinationId, pageable);

        return toPage(result.map(r -> toRestaurantResponse(r, menuItemRepository.countByRestaurantId(r.getId()))));
    }

    @Transactional
    public PageResponse<MenuItemResponse> getMenuItems(Long restaurantId, int page) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> ApiException.notFound("Restaurant not found"));
        ensureMenuItems(restaurant);

        Pageable pageable = PageRequest.of(page, RestaurantCatalogGenerator.PAGE_SIZE);
        Page<MenuItem> result = menuItemRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId, pageable);

        return toPage(result.map(this::toMenuItemResponse));
    }

    @Transactional
    public void ensureRestaurants(Destination destination) {
        if (restaurantRepository.existsByDestinationId(destination.getId())) {
            return;
        }

        List<Restaurant> restaurants;
        String apiKey = appProperties.getGrok().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                restaurants = generateRestaurantsWithGrok(destination, apiKey);
            } catch (Exception e) {
                log.error("Failed to generate restaurants using Grok for destination: {}. Error: {}", destination.getName(), e.getMessage(), e);
                restaurants = RestaurantCatalogGenerator.generateRestaurants(destination);
            }
        } else {
            restaurants = RestaurantCatalogGenerator.generateRestaurants(destination);
        }

        restaurantRepository.saveAll(restaurants);
        for (Restaurant restaurant : restaurants) {
            if (apiKey != null && !apiKey.isBlank()) {
                try {
                    menuItemRepository.saveAll(generateMenuItemsWithGrok(restaurant, apiKey));
                } catch (Exception e) {
                    log.error("Failed to generate menu items using Grok for restaurant: {}. Error: {}", restaurant.getName(), e.getMessage(), e);
                    menuItemRepository.saveAll(RestaurantCatalogGenerator.generateMenuItems(restaurant));
                }
            } else {
                menuItemRepository.saveAll(RestaurantCatalogGenerator.generateMenuItems(restaurant));
            }
        }
    }

    private void ensureMenuItems(Restaurant restaurant) {
        if (menuItemRepository.existsByRestaurantId(restaurant.getId())) {
            return;
        }
        String apiKey = appProperties.getGrok().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                menuItemRepository.saveAll(generateMenuItemsWithGrok(restaurant, apiKey));
                return;
            } catch (Exception e) {
                log.error("Failed to generate menu items using Grok for restaurant: {}. Error: {}", restaurant.getName(), e.getMessage(), e);
            }
        }
        menuItemRepository.saveAll(RestaurantCatalogGenerator.generateMenuItems(restaurant));
    }

    private RestaurantResponse toRestaurantResponse(Restaurant r, long menuCount) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .destinationId(r.getDestination().getId())
                .name(r.getName())
                .cuisine(r.getCuisine())
                .rating(r.getRating())
                .deliveryMinutes(r.getDeliveryMinutes())
                .costForTwo(r.getCostForTwo())
                .imageUrl(r.getImageUrl())
                .menuItemCount(menuCount)
                .build();
    }

    private MenuItemResponse toMenuItemResponse(MenuItem m) {
        return MenuItemResponse.builder()
                .id(m.getId())
                .restaurantId(m.getRestaurant().getId())
                .name(m.getName())
                .description(m.getDescription())
                .price(m.getPrice())
                .rating(m.getRating())
                .veg(m.isVeg())
                .category(m.getCategory())
                .imageUrl(m.getImageUrl())
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

    @SuppressWarnings("unchecked")
    private List<Restaurant> generateRestaurantsWithGrok(Destination destination, String apiKey) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        String prompt = "Generate a list of 15 real popular restaurants in " + city + ".\n" +
                "The response MUST be a JSON object in this format:\n" +
                "{\n" +
                "  \"restaurants\": [\n" +
                "    {\n" +
                "      \"name\": \"Restaurant Name\",\n" +
                "      \"cuisine\": \"Cuisine/Food type\",\n" +
                "      \"rating\": 4.3, // double between 3.5 and 5.0\n" +
                "      \"deliveryMinutes\": 30, // integer between 15 and 60\n" +
                "      \"costForTwo\": 800 // integer between 200 and 3000\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "IMPORTANT: Return ONLY valid raw JSON. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON starting with '{' and ending with '}'.";

        var body = Map.of(
                "model", appProperties.getGrok().getModel(),
                "input", List.of(
                        Map.of("role", "system", "content", "You are a local culinary expert. Return only JSON responses."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 1500
        );

        var response = webClientBuilder.build()
                .post()
                .uri(appProperties.getGrok().getBaseUrl() + "/responses")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty response from Grok Responses API");
        }

        var output = (List<Map<String, Object>>) response.get("output");
        if (output == null || output.isEmpty()) {
            throw new IllegalStateException("No output returned from Grok Responses API");
        }
        var messageMap = output.get(0);
        String jsonText = (String) messageMap.get("content");
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalStateException("Empty content from Grok Responses API");
        }

        List<Restaurant> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            JsonNode restaurantsNode = root.get("restaurants");
            if (restaurantsNode != null && restaurantsNode.isArray()) {
                int index = 0;
                for (JsonNode restNode : restaurantsNode) {
                    String name = restNode.has("name") ? restNode.get("name").asText() : "Restaurant in " + city;
                    String cuisine = restNode.has("cuisine") ? restNode.get("cuisine").asText() : "Multi-cuisine";
                    double rating = restNode.has("rating") ? restNode.get("rating").asDouble() : 4.2;
                    int deliveryMinutes = restNode.has("deliveryMinutes") ? restNode.get("deliveryMinutes").asInt() : 30;
                    int costForTwo = restNode.has("costForTwo") ? restNode.get("costForTwo").asInt() : 600;

                    // Generate custom image for restaurant and download/cache it
                    String imgPrompt = "A beautiful photo of " + name + " restaurant in " + city + ", cozy restaurant dining interior or storefront facade, realistic food photography.";
                    String remoteImgUrl = destinationService.generateGrokImageUrl(imgPrompt);
                    String localImgUrl = destinationService.downloadImage(remoteImgUrl, "restaurant_" + index);
                    if (localImgUrl == null) {
                        localImgUrl = "https://images.unsplash.com/featured/400x300/?restaurant,dining&sig=" + index;
                    }

                    list.add(Restaurant.builder()
                            .destination(destination)
                            .name(name)
                            .cuisine(cuisine)
                            .rating(rating)
                            .deliveryMinutes(deliveryMinutes)
                            .costForTwo(costForTwo)
                            .imageUrl(localImgUrl)
                            .rankOrder(index + 1)
                            .build());
                    index++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Grok restaurants response: " + e.getMessage(), e);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private List<MenuItem> generateMenuItemsWithGrok(Restaurant restaurant, String apiKey) {
        String city = restaurant.getDestination().getCity() != null ? restaurant.getDestination().getCity() : restaurant.getDestination().getName();
        String prompt = "Generate a list of 15 authentic menu items or dishes served at the restaurant '" + restaurant.getName() + "' in " + city + " (" + restaurant.getCuisine() + " cuisine).\n" +
                "The response MUST be a JSON object in this format:\n" +
                "{\n" +
                "  \"menuItems\": [\n" +
                "    {\n" +
                "      \"name\": \"Dish Name\",\n" +
                "      \"description\": \"A mouth-watering description of this dish\",\n" +
                "      \"price\": 350.0, // double price in INR (between 50 and 1200)\n" +
                "      \"veg\": true, // boolean\n" +
                "      \"category\": \"One of: Starter, Main, Dessert, Beverage\"\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "IMPORTANT: Return ONLY valid raw JSON. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON starting with '{' and ending with '}'.";

        var body = Map.of(
                "model", appProperties.getGrok().getModel(),
                "input", List.of(
                        Map.of("role", "system", "content", "You are a local culinary expert. Return only JSON responses."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 1500
        );

        var response = webClientBuilder.build()
                .post()
                .uri(appProperties.getGrok().getBaseUrl() + "/responses")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new IllegalStateException("Empty response from Grok Responses API");
        }

        var output = (List<Map<String, Object>>) response.get("output");
        if (output == null || output.isEmpty()) {
            throw new IllegalStateException("No output returned from Grok Responses API");
        }
        var messageMap = output.get(0);
        String jsonText = (String) messageMap.get("content");
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalStateException("Empty content from Grok Responses API");
        }

        List<MenuItem> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            JsonNode itemsNode = root.get("menuItems");
            if (itemsNode != null && itemsNode.isArray()) {
                int index = 0;
                java.util.Random rand = new java.util.Random();
                for (JsonNode itemNode : itemsNode) {
                    String name = itemNode.has("name") ? itemNode.get("name").asText() : "Dish " + index;
                    String description = itemNode.has("description") ? itemNode.get("description").asText() : "";
                    double price = itemNode.has("price") ? itemNode.get("price").asDouble() : 250.0;
                    boolean veg = itemNode.has("veg") ? itemNode.get("veg").asBoolean() : true;
                    String category = itemNode.has("category") ? itemNode.get("category").asText() : "Main";

                    // Generate custom image for dish and download/cache it
                    String imgPrompt = "A delicious, high-quality, professional food photograph of " + name + ", presented beautifully, professional food styling.";
                    String remoteImgUrl = destinationService.generateGrokImageUrl(imgPrompt);
                    String localImgUrl = destinationService.downloadImage(remoteImgUrl, "food_" + index);
                    if (localImgUrl == null) {
                        localImgUrl = "https://images.unsplash.com/featured/400x300/?food,cooking&sig=" + index;
                    }

                    list.add(MenuItem.builder()
                            .restaurant(restaurant)
                            .name(name)
                            .description(description)
                            .price(price)
                            .rating(Math.round((3.8 + rand.nextDouble() * 1.1) * 10.0) / 10.0)
                            .veg(veg)
                            .category(category)
                            .imageUrl(localImgUrl)
                            .sortOrder(index + 1)
                            .build());
                    index++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Grok menu items response: " + e.getMessage(), e);
        }

        return list;
    }
}
