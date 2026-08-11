package com.travelapp.config;

import com.travelapp.entity.Destination;
import com.travelapp.entity.FoodRecommendation;
import com.travelapp.enums.FoodSource;
import com.travelapp.repository.DestinationRepository;
import com.travelapp.repository.FoodRecommendationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final FoodRecommendationRepository foodRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        boolean resetNeeded = false;
        if (destinationRepository.count() > 0) {
            if (destinationRepository.findAll().stream().anyMatch(d -> 
                    d.getName().equalsIgnoreCase("New York City") || 
                    d.getName().equalsIgnoreCase("Santorini"))) {
                resetNeeded = true;
            }
        } else {
            resetNeeded = true;
        }

        if (resetNeeded) {
            log.info("Resetting database for the new Explore page configuration (Paris, Tokyo, Bali, Switzerland)...");
            try {
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE bookings").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE trips").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE reviews").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE food_recommendations").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE menu_items").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE restaurants").executeUpdate();
                entityManager.createNativeQuery("TRUNCATE TABLE destinations").executeUpdate();
                entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
                entityManager.clear();
            } catch (Exception e) {
                log.error("Failed to clean database, attempting soft clean: {}", e.getMessage());
            }
        }

        if (destinationRepository.count() > 0) {
            try {
                List<Destination> allDests = destinationRepository.findAll();
                for (Destination d : allDests) {
                    if (d.getImageUrl() != null && d.getImageUrl().contains("unsplash.com/photo-")) {
                        String query = d.getCity() != null ? d.getCity() : d.getName();
                        String newUrl = "https://images.unsplash.com/featured/800x600/?" + query.toLowerCase().replace(" ", "+") + ",travel,city";
                        d.setImageUrl(newUrl);
                        destinationRepository.save(d);
                        log.info("Repaired legacy image URL for destination: {} -> {}", d.getName(), newUrl);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to repair legacy destination image URLs: {}", e.getMessage());
            }
            log.info("Destinations already seeded, skipping.");
            return;
        }

        log.info("Seeding 4 destinations...");

        List<Destination> destinations = List.of(
                Destination.builder()
                        .name("Paris").country("France").city("Paris")
                        .description("The City of Light — iconic landmarks, world-class cuisine, and romantic ambiance.")
                        .imageUrl("https://images.unsplash.com/featured/800x600/?paris,eiffel")
                        .latitude(48.8566).longitude(2.3522)
                        .climate("Temperate").bestSeason("Spring, Fall")
                        .tags("romance,culture,food,art").build(),
                Destination.builder()
                        .name("Tokyo").country("Japan").city("Tokyo")
                        .description("A vibrant blend of ultramodern and traditional, from neon-lit skyscrapers to historic temples.")
                        .imageUrl("https://images.unsplash.com/featured/800x600/?tokyo,japan")
                        .latitude(35.6762).longitude(139.6503)
                        .climate("Humid subtropical").bestSeason("Spring, Fall")
                        .tags("technology,culture,food,anime").build(),
                Destination.builder()
                        .name("Bali").country("Indonesia").city("Denpasar")
                        .description("Tropical paradise with stunning beaches, lush rice terraces, and spiritual retreats.")
                        .imageUrl("https://images.unsplash.com/featured/800x600/?bali,beach")
                        .latitude(-8.3405).longitude(115.0920)
                        .climate("Tropical").bestSeason("April-October")
                        .tags("beach,wellness,nature,adventure").build(),
                Destination.builder()
                        .name("Switzerland").country("Switzerland").city("Zurich")
                        .description("Breathtaking Alpine landscapes, pristine lakes, charming ski resorts, and world-class chocolates.")
                        .imageUrl("https://images.unsplash.com/featured/800x600/?switzerland,alps")
                        .latitude(47.3769).longitude(8.5417)
                        .climate("Temperate alpine").bestSeason("Winter, Summer")
                        .tags("nature,skiing,chocolate,scenery").build()
        );

        destinations = destinationRepository.saveAll(destinations);

        seedFood(destinations.get(0), "Le Comptoir", "Classic French bistro", "French", 4.8);
        seedFood(destinations.get(1), "Sushi Dai", "Famous Tsukiji market sushi", "Japanese", 4.9);
        seedFood(destinations.get(2), "Locavore", "Farm-to-table Balinese cuisine", "Indonesian", 4.7);
        seedFood(destinations.get(3), "Kronenhalle", "Legendary Zurich art-dining landmark", "Swiss", 4.7);

        log.info("Seeded {} destinations with food recommendations.", destinations.size());
    }

    private void seedFood(Destination dest, String name, String desc, String cuisine, double rating) {
        foodRepository.save(FoodRecommendation.builder()
                .destination(dest)
                .name(name)
                .description(desc)
                .cuisine(cuisine)
                .rating(rating)
                .priceRange("$$")
                .address(dest.getCity() + ", " + dest.getCountry())
                .source(FoodSource.LOCAL)
                .build());
    }
}
