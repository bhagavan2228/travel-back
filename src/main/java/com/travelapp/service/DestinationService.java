package com.travelapp.service;

import com.travelapp.catalog.LocationCatalog;
import com.travelapp.dto.destination.DestinationResponse;
import com.travelapp.dto.destination.HotelResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.Hotel;
import com.travelapp.exception.ApiException;
import com.travelapp.mapper.EntityMapper;
import com.travelapp.repository.DestinationRepository;
import com.travelapp.repository.HotelRepository;
import com.travelapp.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private static final Logger log = LoggerFactory.getLogger(DestinationService.class);

    private final DestinationRepository destinationRepository;
    private final HotelRepository hotelRepository;
    private final AppProperties appProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DestinationResponse> findAll() {
        return destinationRepository.findAll().stream()
                .map(EntityMapper::toDestinationResponse)
                .toList();
    }

    @Transactional
    public DestinationResponse findById(Long id) {
        Destination dest = getDestination(id);
        dest.setExploredCount(dest.getExploredCount() + 1);
        destinationRepository.save(dest);
        return EntityMapper.toDestinationResponse(dest);
    }

    @Transactional
    public List<DestinationResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        String normalized = query.trim();
        Map<Long, Destination> found = new LinkedHashMap<>();

        for (String term : splitSearchTerms(normalized)) {
            destinationRepository.search(term).forEach(d -> found.put(d.getId(), d));
        }
        destinationRepository.search(normalized).forEach(d -> found.put(d.getId(), d));

        if (found.isEmpty()) {
            discoverFromCatalog(normalized).forEach(d -> found.put(d.getId(), d));
        }

        if (found.isEmpty()) {
            Destination dynamic = generateDynamicDestination(normalized);
            found.put(dynamic.getId(), dynamic);
        }

        found.values().forEach(dest -> {
            dest.setExploredCount(dest.getExploredCount() + 1);
            destinationRepository.save(dest);
        });

        return found.values().stream()
                .map(EntityMapper::toDestinationResponse)
                .toList();
    }

    private List<String> splitSearchTerms(String query) {
        List<String> terms = new ArrayList<>();
        for (String part : query.split("[,]+")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                terms.add(trimmed);
            }
        }
        return terms;
    }

    @Transactional
    public List<Destination> discoverFromCatalog(String query) {
        List<Destination> saved = new ArrayList<>();
        for (LocationCatalog catalog : LocationCatalog.matchQuery(query)) {
            if (destinationRepository.existsByCityIgnoreCaseAndCountryIgnoreCase(
                    catalog.getCity(), catalog.getCountry())) {
                destinationRepository.findByCityOrNameContaining(catalog.getCity()).stream()
                        .filter(d -> d.getCountry().equalsIgnoreCase(catalog.getCountry()))
                        .findFirst()
                        .ifPresent(saved::add);
                continue;
            }

            String finalImageUrl = catalog.getImageUrl();
            String apiKey = appProperties.getGrok().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                try {
                    String imgPrompt = "A breathtaking, professional travel photograph of the main landmark of " + catalog.getCity() + ", " + catalog.getCountry() + ", wide shot, beautiful lighting, cinematic composition.";
                    String remoteImgUrl = generateGrokImageUrl(imgPrompt);
                    String localImgUrl = downloadImage(remoteImgUrl, "destination_" + catalog.getCity().toLowerCase().replace(" ", "_"));
                    if (localImgUrl != null) {
                        finalImageUrl = localImgUrl;
                    }
                } catch (Exception e) {
                    log.error("Failed to generate image via Grok for catalog destination: {}. Error: {}", catalog.getName(), e.getMessage());
                }
            }

            Destination destination = Destination.builder()
                    .name(catalog.getName())
                    .city(catalog.getCity())
                    .state(catalog.getState())
                    .country(catalog.getCountry())
                    .description(catalog.getDescription())
                    .imageUrl(finalImageUrl)
                    .latitude(catalog.getLatitude())
                    .longitude(catalog.getLongitude())
                    .tags(catalog.getTags())
                    .climate("Varies")
                    .bestSeason("Oct–Mar")
                    .exploredCount(0)
                    .build();
            saved.add(destinationRepository.save(destination));
        }
        return saved;
    }

    @Transactional
    public Destination generateDynamicDestination(String query) {
        String capitalized = capitalize(query);
        String city = capitalized;
        String country = "India";
        if (query.contains(",")) {
            String[] parts = query.split(",");
            city = capitalize(parts[0].trim());
            country = capitalize(parts[1].trim());
        }

        String apiKey = appProperties.getGrok().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            return destinationRepository.save(generateDestinationWithGrok(city, country));
        }

        String description = "Explore the wonderful city of " + city + ", " + country + ". Famous for its vibrant local culture, stunning architectural landmarks, scenic beauty, and authentic regional cuisine. Popular must-visit attractions include historical monuments, serene gardens, bustling local markets, and local food spots. A perfect getaway.";
        String lower = city.toLowerCase();
        String imageUrl;
        
        if (lower.contains("warangal")) {
            description = "Explore the historic city of Warangal, Telangana. Famous for its magnificent Kakatiya heritage, the iconic Thousand Pillar Temple, the majestic Warangal Fort, and the serene Bhadrakali Lake. A perfect cultural journey highlighting Kakatiya architecture, beautiful stone carvings, and traditional Telugu heritage.";
            imageUrl = "https://images.unsplash.com/photo-1605649487212-47bdab064df7?w=800&q=80";
        } else if (lower.contains("araku")) {
            description = "Discover the scenic beauty of Araku Valley, Andhra Pradesh. Famous for its lush coffee plantations, pleasant climate, breathtaking waterfalls (such as Chaparai), and the stunning Borra Caves. A perfect hill station getaway for nature lovers, adventure enthusiasts, and coffee connoisseurs.";
            imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&q=80";
        } else if (lower.contains("goa")) {
            description = "Welcome to Goa, the coastal paradise of India. Famous for its sandy beaches, vibrant nightlife, Portuguese heritage churches, spice plantations, and delicious seafood. Ideal for water sports, beach relaxation, and heritage exploration.";
            imageUrl = "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800&q=80";
        } else if (lower.contains("kerala")) {
            description = "Explore Kerala, God's Own Country. Famous for its tranquil backwaters, traditional houseboats, lush tea gardens of Munnar, diverse wildlife at Periyar, and beautiful beaches of Kovalam. Ideal for nature retreats and Ayurvedic relaxation.";
            imageUrl = "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?w=800&q=80";
        } else if (lower.contains("delhi")) {
            description = "Explore New Delhi, the capital city of India. Rich in history and politics, it features massive Mughal monuments like the Red Fort, Qutub Minar, and Humayun's Tomb alongside the grand colonial government buildings of Lutyens. Perfect for street food lovers and shoppers.";
            imageUrl = "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=800&q=80";
        } else {
            String searchUrl = lower.replace(" ", "+");
            imageUrl = "https://images.unsplash.com/featured/800x600/?" + searchUrl + ",travel,city";
        }

        Destination destination = Destination.builder()
                .name(city)
                .city(city)
                .country(country)
                .description(description)
                .imageUrl(imageUrl)
                .latitude(17.97)
                .longitude(79.60)
                .climate("Tropical / Pleasant")
                .bestSeason("October to March")
                .tags("culture, nature, history, exploration")
                .exploredCount(0)
                .build();

        return destinationRepository.save(destination);
    }

    @SuppressWarnings("unchecked")
    private Destination generateDestinationWithGrok(String city, String country) {
        String apiKey = appProperties.getGrok().getApiKey();
        String prompt = "Generate information for a travel guide app about the destination: " + city + ", " + country + ".\n" +
                "The response MUST be a JSON object in this format:\n" +
                "{\n" +
                "  \"description\": \"A beautiful, detailed paragraph describing key facts, culture, and highlights of " + city + "\",\n" +
                "  \"climate\": \"E.g., Tropical, Temperate, Warm & Humid\",\n" +
                "  \"bestSeason\": \"E.g., October to March, Year-round\",\n" +
                "  \"tags\": \"Comma-separated lowercase tags, e.g., culture, history, adventure, nature\"\n" +
                "}\n" +
                "IMPORTANT: Return ONLY valid raw JSON. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON starting with '{' and ending with '}'.";

        String description = "Explore the wonderful city of " + city + ", " + country + ". Famous for its local culture and attractions.";
        String climate = "Pleasant";
        String bestSeason = "October to March";
        String tags = "culture, nature, exploration";

        try {
            var body = Map.of(
                    "model", appProperties.getGrok().getModel(),
                    "input", List.of(
                            Map.of("role", "system", "content", "You are a helpful travel guide expert. Return only JSON responses."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "max_tokens", 800
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

            if (response != null) {
                var output = (List<Map<String, Object>>) response.get("output");
                if (output != null && !output.isEmpty()) {
                    String jsonText = (String) output.get(0).get("content");
                    JsonNode root = objectMapper.readTree(jsonText);
                    if (root.has("description")) description = root.get("description").asText();
                    if (root.has("climate")) climate = root.get("climate").asText();
                    if (root.has("bestSeason")) bestSeason = root.get("bestSeason").asText();
                    if (root.has("tags")) tags = root.get("tags").asText();
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate dynamic destination info via Grok: {}", e.getMessage());
        }

        // Generate Custom Image
        String imgPrompt = "A breathtaking, professional travel photograph of the main landmark of " + city + ", " + country + ", wide shot, beautiful lighting, cinematic composition.";
        String remoteImgUrl = generateGrokImageUrl(imgPrompt);
        String localImgUrl = downloadImage(remoteImgUrl, "destination_" + city.toLowerCase().replace(" ", "_"));
        if (localImgUrl == null) {
            localImgUrl = "https://images.unsplash.com/featured/800x600/?" + city.toLowerCase().replace(" ", "+") + ",travel,city";
        }

        return Destination.builder()
                .name(city)
                .city(city)
                .country(country)
                .description(description)
                .imageUrl(localImgUrl)
                .latitude(17.97)
                .longitude(79.60)
                .climate(climate)
                .bestSeason(bestSeason)
                .tags(tags)
                .exploredCount(0)
                .build();
    }

    public String downloadImage(String sourceUrl, String prefix) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = webClientBuilder.build()
                    .get()
                    .uri(sourceUrl)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            String fileName = prefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000) + ".png";
            
            // Write to target (immediate serving)
            java.nio.file.Path targetDirPath = java.nio.file.Paths.get("target/classes/static/images");
            java.nio.file.Files.createDirectories(targetDirPath);
            java.nio.file.Files.write(targetDirPath.resolve(fileName), bytes);

            // Write to src (persistence)
            java.nio.file.Path srcDirPath = java.nio.file.Paths.get("src/main/resources/static/images");
            java.nio.file.Files.createDirectories(srcDirPath);
            java.nio.file.Files.write(srcDirPath.resolve(fileName), bytes);

            return "/api/images/" + fileName;
        } catch (Exception e) {
            log.error("Failed to download image from {}: {}", sourceUrl, e.getMessage());
            return sourceUrl; // Fallback to raw URL if download fails
        }
    }

    @SuppressWarnings("unchecked")
    public String generateGrokImageUrl(String prompt) {
        String apiKey = appProperties.getGrok().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            var body = Map.of(
                    "model", "grok-imagine-image-quality",
                    "prompt", prompt,
                    "n", 1
            );
            var response = webClientBuilder.build()
                    .post()
                    .uri(appProperties.getGrok().getBaseUrl() + "/images/generations")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response != null && response.containsKey("data")) {
                var data = (List<Map<String, Object>>) response.get("data");
                if (data != null && !data.isEmpty()) {
                    return (String) data.get(0).get("url");
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate Grok image for prompt: {}. Error: {}", prompt, e.getMessage());
        }
        return null;
    }

    @Transactional
    public List<HotelResponse> getHotels(Long destinationId) {
        Destination destination = getDestination(destinationId);
        List<Hotel> hotels = hotelRepository.findByDestinationId(destinationId);
        if (hotels.isEmpty()) {
            String apiKey = appProperties.getGrok().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                try {
                    hotels = generateHotelsWithGrok(destination, apiKey);
                } catch (Exception e) {
                    log.error("Failed to generate hotels using Grok for destination: {}. Error: {}", destination.getName(), e.getMessage(), e);
                    hotels = generateMockHotels(destination);
                }
            } else {
                hotels = generateMockHotels(destination);
            }
            hotelRepository.saveAll(hotels);
        }

        return hotels.stream()
                .map(h -> HotelResponse.builder()
                        .id(h.getId())
                        .name(h.getName())
                        .imageUrl(h.getImageUrl())
                        .rating(h.getRating())
                        .price(h.getPrice())
                        .vacancies(h.getVacancies())
                        .reviews(h.getReviews())
                        .build())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Hotel> generateHotelsWithGrok(Destination destination, String apiKey) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        String prompt = "Generate a list of 10 real or highly realistic popular hotels in " + city + ".\n" +
                "The response MUST be a JSON object in this format:\n" +
                "{\n" +
                "  \"hotels\": [\n" +
                "    {\n" +
                "      \"name\": \"Hotel Name\",\n" +
                "      \"rating\": 4.5, // double between 3.5 and 5.0\n" +
                "      \"price\": 3500, // integer price in INR per night (between 1500 and 15000)\n" +
                "      \"vacancies\": 5, // integer rooms left (between 0 and 10)\n" +
                "      \"reviews\": [\n" +
                "        \"Excellent stay, close to city center\",\n" +
                "        \"Friendly staff and spacious clean rooms\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "IMPORTANT: Return ONLY valid raw JSON. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON starting with '{' and ending with '}'.";

        var body = Map.of(
                "model", appProperties.getGrok().getModel(),
                "input", List.of(
                        Map.of("role", "system", "content", "You are a local travel expert. Return only JSON responses."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 1200
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

        List<Hotel> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            JsonNode hotelsNode = root.get("hotels");
            if (hotelsNode != null && hotelsNode.isArray()) {
                int index = 0;
                for (JsonNode hotelNode : hotelsNode) {
                    String name = hotelNode.has("name") ? hotelNode.get("name").asText() : "Hotel in " + city;
                    double rating = hotelNode.has("rating") ? hotelNode.get("rating").asDouble() : 4.2;
                    int price = hotelNode.has("price") ? hotelNode.get("price").asInt() : 3000;
                    int vacancies = hotelNode.has("vacancies") ? hotelNode.get("vacancies").asInt() : 3;
                    
                    List<String> reviews = new ArrayList<>();
                    if (hotelNode.has("reviews") && hotelNode.get("reviews").isArray()) {
                        for (JsonNode revNode : hotelNode.get("reviews")) {
                            reviews.add(revNode.asText());
                        }
                    }

                    // Generate custom image of hotel and cache it locally
                    String imgPrompt = "A beautiful photo of " + name + " hotel in " + city + ", elegant building exterior facade, realistic architecture photography, sunny day.";
                    String remoteImgUrl = generateGrokImageUrl(imgPrompt);
                    String localImgUrl = downloadImage(remoteImgUrl, "hotel_" + index);
                    if (localImgUrl == null) {
                        localImgUrl = "https://images.unsplash.com/featured/400x300/?hotel,room&sig=" + index;
                    }

                    list.add(Hotel.builder()
                            .destination(destination)
                            .name(name)
                            .imageUrl(localImgUrl)
                            .rating(rating)
                            .price(price)
                            .vacancies(vacancies)
                            .reviews(reviews)
                            .build());
                    index++;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Grok hotels response: " + e.getMessage(), e);
        }

        return list;
    }

    private List<Hotel> generateMockHotels(Destination destination) {
        String city = destination.getCity() != null ? destination.getCity() : destination.getName();
        List<Hotel> list = new ArrayList<>();
        String[][] templates = {
            {"Grand Palace & Spa", "4.7", "5500", "luxury"},
            {"Royal Heritage Mansion", "4.5", "4200", "heritage"},
            {"Urban Comfort Suites", "4.1", "3200", "business"},
            {"Boutique Oasis", "4.3", "3800", "boutique"},
            {"Serene Valley Resort", "4.4", "4800", "nature"},
            {"Central Inn & Suites", "4.0", "2800", "business"},
            {"Lakeside Manor", "4.2", "3400", "nature"},
            {"Budget Stay Lodge", "3.8", "1800", "budget"},
            {"Pinewood Residency", "4.1", "2900", "nature"},
            {"Metropolitan Plaza", "4.2", "3100", "business"}
        };
        for (int i = 0; i < templates.length; i++) {
            String name = city + " " + templates[i][0];
            double rating = Double.parseDouble(templates[i][1]);
            int price = Integer.parseInt(templates[i][2]);
            int vacancies = i % 3 == 0 ? 0 : 3 + (i % 6);
            String imageUrl = "https://images.unsplash.com/featured/400x300/?hotel,room," + templates[i][3] + "&sig=" + i;

            list.add(Hotel.builder()
                    .destination(destination)
                    .name(name)
                    .imageUrl(imageUrl)
                    .rating(rating)
                    .price(price)
                    .vacancies(vacancies)
                    .reviews(List.of(
                        "Extremely clean rooms and hospitable staff. The food was top tier!",
                        "Spectacular view from the balcony. Perfect weekend getaway."
                    ))
                    .build());
        }
        return list;
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public Destination getDestination(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Destination not found"));
    }
}
