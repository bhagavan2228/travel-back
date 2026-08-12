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
import com.travelapp.service.integration.WikipediaImageClient;
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
    private final WikipediaImageClient wikipediaImageClient;
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

        String description = "Explore the wonderful city of " + city + ", " + country + ". " +
                "Famous for its vibrant local culture, stunning architectural landmarks, scenic natural beauty, and authentic regional cuisine. " +
                "Popular must-visit attractions include historical monuments, ancient temples, serene gardens, bustling local markets, and famous local food spots. " +
                "The city offers a rich tapestry of history, heritage, and modern development. " +
                "Visitors can enjoy traditional festivals, local craftsmanship, and explore nearby natural attractions. " +
                "The local food scene features unique regional specialties that have been perfected over generations. " +
                "Transportation is well-connected with major cities through rail, road, and air. " +
                "The best time to visit is during the cooler months for a comfortable sightseeing experience. " +
                "A perfect getaway for history enthusiasts, food lovers, and culture seekers alike.";
        String lower = city.toLowerCase();
        String imageUrl;
        
        if (lower.contains("warangal")) {
            description = "Warangal, the historic Kakatiya capital, is a treasure trove of medieval Indian art and architecture in Telangana. " +
                    "The Thousand Pillar Temple, built in 1163 AD by King Rudra Deva, is a masterpiece of Kakatiya architecture featuring intricately carved pillars dedicated to Shiva, Vishnu, and Surya. " +
                    "Warangal Fort, a UNESCO World Heritage tentative site, showcases magnificent stone gateways (Kirti Thoranas) with exquisite carvings that rival any in India. " +
                    "Ramappa Temple (Ramalingeswara Temple), a UNESCO World Heritage Site, is an engineering marvel with floating bricks and stunning Kakatiyan sculptural art. " +
                    "Bhadrakali Lake, surrounding the ancient Bhadrakali Temple, offers serene boating and picnic spots. " +
                    "Pakhal Lake, a 13th-century man-made lake surrounded by dense forest, is a wildlife sanctuary and birding paradise. " +
                    "Warangal's cuisine features authentic Telangana flavors — Sakinalu (crispy rice snack), Jonna Rotte (sorghum flatbread), Golichina Mamsam (spicy mutton curry), and Sarva Pindi. " +
                    "The Kakatiya Musical Garden offers a colorful fountain show set to music. " +
                    "The Warangal Fort's iconic four Kirti Thoranas (ornamental arches) are featured on the Telangana state emblem. " +
                    "Eturnagaram Wildlife Sanctuary, one of the oldest in Telangana, is home to tigers, leopards, and diverse birdlife. " +
                    "The city was once known as Orugallu (single stone), referring to the massive boulder on which the fort was built. " +
                    "Thousand Pillar Temple's Nandi statue is carved from a single block of black basalt and is considered a sculptural marvel. " +
                    "The Sammakka Saralamma Jatara festival near Warangal is the largest tribal fair in Asia, attracting millions. " +
                    "Warangal has a semi-arid climate with hot summers; the best time to visit is October to February. " +
                    "The city is well-connected by rail and road, approximately 150 km from Hyderabad, and is developing rapidly as an educational and IT hub.";
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Thousand_Pillar_Temple_2.jpg/800px-Thousand_Pillar_Temple_2.jpg";
        } else if (lower.contains("araku")) {
            description = "Araku Valley, nestled in the Eastern Ghats of Andhra Pradesh, is a breathtaking hill station famous for its coffee plantations, tribal culture, and stunning natural beauty. " +
                    "The valley sits at an elevation of 900 meters and is known for its pleasant climate, lush greenery, and rolling hills. " +
                    "Borra Caves, one of the largest caves in India, feature stunning million-year-old stalactite and stalagmite formations. " +
                    "The Araku Coffee is considered among the finest in the world, organically grown by tribal farmers and internationally awarded. " +
                    "Chaparai Waterfalls, a spectacular cascading waterfall surrounded by dense forests, is a must-visit during monsoons. " +
                    "The Tribal Museum showcases the rich culture, art, and lifestyle of the local Dhimsa and other tribal communities. " +
                    "Padmapuram Gardens, with its hanging trees, topiary art, and flower gardens, is a delightful retreat. " +
                    "The train journey from Visakhapatnam to Araku through 58 tunnels and over scenic bridges is one of the most beautiful rail journeys in India. " +
                    "Local cuisine includes bamboo chicken (chicken cooked inside bamboo over fire), bamboo biryani, and authentic tribal preparations. " +
                    "Ananthagiri Hills, on the way to Araku, offer panoramic viewpoints and coffee estate walks. " +
                    "The valley's Dhimsa dance, performed by tribal communities, is a UNESCO-recognized cultural tradition. " +
                    "Araku hosts an annual coffee festival celebrating its world-class Arabica coffee. " +
                    "Katiki Waterfalls, hidden in a cave formation, provides a unique trekking and adventure experience. " +
                    "The best time to visit Araku is October to March when the weather is cool and pleasant. " +
                    "The valley is approximately 115 km from Visakhapatnam and is accessible by road and the scenic Kirandul-Visakhapatnam railway line.";
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b5/Araku_Valley.jpg/800px-Araku_Valley.jpg";
        } else if (lower.contains("goa")) {
            description = "Goa, India's smallest state by area, is the country's premier beach destination and a melting pot of Indian and Portuguese cultures. " +
                    "Baga Beach and Calangute Beach are the most popular tourist beaches, known for their vibrant nightlife, beach shacks, and water sports. " +
                    "Palolem Beach in South Goa is a crescent-shaped paradise ideal for kayaking, dolphin watching, and silent noise parties. " +
                    "The Basilica of Bom Jesus, a UNESCO World Heritage Site, houses the mortal remains of St. Francis Xavier. " +
                    "Goa's cuisine is a unique Indo-Portuguese fusion — Fish Curry Rice, Bebinca, Vindaloo, Xacuti, and Prawn Balchão are must-try dishes. " +
                    "Feni, locally distilled from cashew or coconut, is Goa's signature spirit. " +
                    "Dudhsagar Falls, a magnificent 310-meter waterfall, is a spectacular monsoon attraction. " +
                    "Fort Aguada offers panoramic views of the Arabian Sea. " +
                    "Anjuna Flea Market and Saturday Night Market at Arpora are iconic shopping experiences. " +
                    "Old Goa was once called the 'Rome of the East'. " +
                    "Goa's carnival in February is a colorful three-day festival. " +
                    "The best time to visit is November to February for beaches. " +
                    "The Goan trance music scene has made Goa world-famous in electronic music culture.";
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Palolem_Beach.jpg/800px-Palolem_Beach.jpg";
        } else if (lower.contains("kerala")) {
            description = "Kerala, God's Own Country, is one of India's most beautiful states, famous for its tranquil backwaters, lush tea gardens, diverse wildlife, and rich Ayurvedic traditions. " +
                    "The serene backwaters of Alleppey offer unforgettable houseboat cruises through palm-fringed canals and lagoons. " +
                    "Munnar's rolling tea plantations, misty peaks, and the Eravikulam National Park (home to the endangered Nilgiri Tahr) make it a premier hill station. " +
                    "Periyar Wildlife Sanctuary in Thekkady offers bamboo rafting, jungle treks, and tiger spotting. " +
                    "Kovalam and Varkala beaches are famous for their dramatic cliffs, lighthouse views, and Ayurvedic beach resorts. " +
                    "Kerala cuisine is a feast — Sadhya (banana leaf meal), Karimeen Pollichathu (pearl spot fish), Appam with Stew, and Payasam are legendary. " +
                    "Fort Kochi features historic Chinese Fishing Nets, colonial architecture, and the Paradesi Synagogue. " +
                    "Kathakali dance drama and Kalaripayattu martial art are signature cultural experiences. " +
                    "Wayanad's misty hills, ancient caves, and spice plantations offer an offbeat retreat. " +
                    "Kerala's Ayurvedic wellness tradition attracts health tourists from around the world. " +
                    "The Snake Boat Race (Vallam Kali) in Alleppey is one of the most thrilling water sports events in India. " +
                    "Kerala has a tropical monsoon climate; the best time to visit is September to March. " +
                    "The state has the highest literacy rate in India and is known for its progressive social indicators.";
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/Houseboats_at_Kerala_Backwaters.jpg/800px-Houseboats_at_Kerala_Backwaters.jpg";
        } else if (lower.contains("delhi")) {
            description = "Delhi, India's sprawling capital territory, is a mesmerizing blend of ancient heritage and modern ambition spanning over 3,000 years of history. " +
                    "The Red Fort, a UNESCO World Heritage Site built by Shah Jahan in 1639, is where India's Independence Day flag hoisting takes place. " +
                    "Qutub Minar, the tallest brick minaret in the world at 72.5 meters, dates back to the 12th century. " +
                    "Humayun's Tomb inspired the design of the Taj Mahal. India Gate is a 42-meter war memorial and the heart of Lutyens' Delhi. " +
                    "Chandni Chowk is a paradise for street food — parantha, jalebi, and chaat are legendary here. " +
                    "The Lotus Temple and Akshardham Temple are stunning modern landmarks. " +
                    "Jama Masjid is one of the largest mosques in India. The Delhi Metro covers over 390 km. " +
                    "Hauz Khas Village combines medieval ruins with trendy cafes. " +
                    "Delhi's climate has extreme seasons; the best time to visit is October to March. " +
                    "Lodhi Garden and Connaught Place are beloved gathering spots. " +
                    "The National Museum and Crafts Museum offer deep cultural immersion.";
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Qutab_Minar_mbread.jpg/800px-Qutab_Minar_mread.jpg";
        } else {
            imageUrl = wikipediaImageClient.fetchImageForQuery(city);
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
        String prompt = "Generate detailed travel guide information for the destination: " + city + ", " + country + ".\n" +
                "The description must be a single long paragraph covering AT LEAST 15-20 detailed points about this place including:\n" +
                "- History and heritage\n" +
                "- At least 5 famous landmarks and tourist attractions\n" +
                "- At least 3 local cuisine specialties with specific dish names\n" +
                "- Cultural highlights and festivals\n" +
                "- Best activities and experiences\n" +
                "- Climate and best times to visit\n" +
                "- Transportation tips\n" +
                "- Local markets and shopping\n" +
                "- Interesting facts\n" +
                "The response MUST be a JSON object in this format:\n" +
                "{\n" +
                "  \"description\": \"A detailed paragraph (at least 800 characters) covering all the above points about " + city + "\",\n" +
                "  \"climate\": \"E.g., Tropical, Temperate, Warm & Humid\",\n" +
                "  \"bestSeason\": \"E.g., October to March, Year-round\",\n" +
                "  \"tags\": \"Comma-separated lowercase tags, e.g., culture, history, adventure, nature\"\n" +
                "}\n" +
                "IMPORTANT: Return ONLY valid raw JSON. Do not wrap it in markdown code blocks like ```json ... ```. Just return the JSON starting with '{' and ending with '}'.";

        String description = "Explore the wonderful city of " + city + ", " + country + ". " +
                "Famous for its vibrant local culture, stunning architectural landmarks, scenic natural beauty, and authentic regional cuisine. " +
                "Popular must-visit attractions include historical monuments, ancient temples, serene gardens, bustling local markets, and famous local food spots. " +
                "The city offers a rich tapestry of history, heritage, and modern development. " +
                "Visitors can enjoy traditional festivals, local craftsmanship, and explore nearby natural attractions. " +
                "The local food scene features unique regional specialties that have been perfected over generations. " +
                "Transportation is well-connected with major cities through rail, road, and air. " +
                "The best time to visit is during the cooler months for a comfortable sightseeing experience. " +
                "A perfect getaway for history enthusiasts, food lovers, and culture seekers alike.";
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
            localImgUrl = wikipediaImageClient.fetchImageForQuery(city);
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
                        localImgUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1e/Hotel_Negresco_2.jpg/800px-Hotel_Negresco_2.jpg";
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
            String imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Hotel_room_in_Paris.jpg/800px-Hotel_room_in_Paris.jpg";

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
