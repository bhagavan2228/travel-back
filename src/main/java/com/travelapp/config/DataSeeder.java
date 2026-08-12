package com.travelapp.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.entity.Destination;
import com.travelapp.repository.DestinationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${google.places-api-key:}")
    private String googlePlacesApiKey;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (destinationRepository.count() > 0) {
            log.info("Destinations already seeded, skipping.");
            return;
        }

        log.info("Seeding destinations...");

        List<Destination> destinations = new ArrayList<>();

        if (googlePlacesApiKey != null && !googlePlacesApiKey.isBlank()) {
            log.info("Google Places API key found. Fetching dynamic destinations...");
            try {
                String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=top+tourist+destinations+in+the+world&key=" + googlePlacesApiKey;
                String response = webClientBuilder.build().get().uri(url).retrieve().bodyToMono(String.class).block();
                JsonNode root = objectMapper.readTree(response);
                JsonNode results = root.path("results");
                
                int count = 0;
                for (JsonNode node : results) {
                    if (count >= 10) break; // Limit to 10
                    
                    String name = node.path("name").asText();
                    String address = node.path("formatted_address").asText();
                    double lat = node.path("geometry").path("location").path("lat").asDouble();
                    double lng = node.path("geometry").path("location").path("lng").asDouble();
                    
                    // Basic split for city/country
                    String[] parts = address.split(",");
                    String city = name;
                    String country = parts.length > 1 ? parts[parts.length - 1].trim() : "Unknown";
                    
                    String photoRef = "";
                    if (node.path("photos").isArray() && node.path("photos").size() > 0) {
                        photoRef = node.path("photos").get(0).path("photo_reference").asText();
                    }
                    String imageUrl = photoRef.isEmpty() 
                        ? "https://images.unsplash.com/featured/800x600/?" + name.toLowerCase().replace(" ", "+") + ",travel"
                        : "https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photoreference=" + photoRef + "&key=" + googlePlacesApiKey;

                    destinations.add(Destination.builder()
                            .name(name)
                            .country(country)
                            .city(city)
                            .description(address)
                            .imageUrl(imageUrl)
                            .latitude(lat)
                            .longitude(lng)
                            .climate("Varied")
                            .bestSeason("Year-round")
                            .tags("dynamic,travel,places")
                            .build());
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to fetch destinations from Google Places API. Falling back to static data.", e);
            }
        }

        if (destinations.isEmpty()) {
            log.info("Falling back to static destination data.");
            destinations = List.of(
                    Destination.builder()
                            .name("Paris").country("France").city("Paris")
                            .description("The City of Light.")
                            .imageUrl("https://images.unsplash.com/featured/800x600/?paris,travel")
                            .latitude(48.8566).longitude(2.3522)
                            .climate("Temperate").bestSeason("Spring, Fall")
                            .tags("romance,culture,food,art").build()
            );
        }

        destinationRepository.saveAll(destinations);
        log.info("Seeded {} destinations.", destinations.size());
    }
}
