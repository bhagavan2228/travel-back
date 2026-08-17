package com.travelapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.dto.car.CarSearchResponse;
import com.travelapp.dto.car.CarSearchResultDto;
import com.travelapp.entity.CarSearchCache;
import com.travelapp.repository.CarSearchCacheRepository;
import com.travelapp.service.integration.DuffelCarApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final DuffelCarApiClient duffelCarApiClient;
    private final CarSearchCacheRepository carCacheRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CarSearchResponse searchCars(String location, String pickupDate, String dropoffDate) {
        String cacheKey = String.format("car|%s|%s|%s", location.toUpperCase(), pickupDate, dropoffDate);

        // 1. Check cache (1-hour TTL)
        carCacheRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusHours(1));
        Optional<CarSearchCache> cached = carCacheRepository.findById(cacheKey);

        if (cached.isPresent()) {
            try {
                CarSearchResponse response = objectMapper.readValue(cached.get().getJsonResponse(), CarSearchResponse.class);
                response.setCached(true);
                return response;
            } catch (Exception e) {
                log.warn("Failed to parse cached car data: {}", e.getMessage());
            }
        }

        // 2. Call Duffel API
        String jsonResponse = duffelCarApiClient.searchCars(location, pickupDate, dropoffDate);
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return getFallbackCars(location, pickupDate, dropoffDate);
        }

        // 3. Parse Duffel API Response (if it actually returns data)
        // Since Duffel might not return car data with this token (403 Forbidden), 
        // we fallback immediately to the mock data if parsing fails or if we get an empty array.
        // We will just try to map it, but fallback if it crashes.
        try {
            // Placeholder parsing logic, depending on Duffel's actual schema
            // For now, if we reach here and it's not JSON, we throw.
            if (!jsonResponse.contains("\"data\"")) {
                throw new RuntimeException("Invalid Duffel Cars API response");
            }
            
            // In a fully working Duffel Cars API, we would map the JSON nodes here.
            // But since we are likely hitting 403, we rely on the fallback below.
            return getFallbackCars(location, pickupDate, dropoffDate);
            
        } catch (Exception e) {
            log.error("Failed to parse Duffel Car API response", e);
            return getFallbackCars(location, pickupDate, dropoffDate);
        }
    }

    private CarSearchResponse getFallbackCars(String location, String pickupDate, String dropoffDate) {
        log.info("Generating fallback car rentals for {}", location);
        
        List<CarSearchResultDto> cars = List.of(
                CarSearchResultDto.builder()
                        .carId("car_123")
                        .brand("Hertz")
                        .vehicleName("Toyota Corolla or similar")
                        .type("Compact")
                        .seats(5)
                        .transmission("Automatic")
                        .pricePerDay(45.0)
                        .priceFormatted("€45.00")
                        .currency("EUR")
                        .imageUrl("https://images.unsplash.com/photo-1590362891991-f776e747a588?auto=format&fit=crop&w=800&q=80")
                        .build(),
                CarSearchResultDto.builder()
                        .carId("car_124")
                        .brand("Avis")
                        .vehicleName("BMW 3 Series or similar")
                        .type("Premium")
                        .seats(5)
                        .transmission("Automatic")
                        .pricePerDay(95.0)
                        .priceFormatted("€95.00")
                        .currency("EUR")
                        .imageUrl("https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=800&q=80")
                        .build(),
                CarSearchResultDto.builder()
                        .carId("car_125")
                        .brand("Enterprise")
                        .vehicleName("Ford Escape or similar")
                        .type("SUV")
                        .seats(5)
                        .transmission("Automatic")
                        .pricePerDay(75.0)
                        .priceFormatted("€75.00")
                        .currency("EUR")
                        .imageUrl("https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?auto=format&fit=crop&w=800&q=80")
                        .build(),
                CarSearchResultDto.builder()
                        .carId("car_126")
                        .brand("Sixt")
                        .vehicleName("Volkswagen Polo or similar")
                        .type("Economy")
                        .seats(4)
                        .transmission("Manual")
                        .pricePerDay(35.0)
                        .priceFormatted("€35.00")
                        .currency("EUR")
                        .imageUrl("https://images.unsplash.com/photo-1541899481282-d53bffe3c35d?auto=format&fit=crop&w=800&q=80")
                        .build()
        );

        CarSearchResponse response = CarSearchResponse.builder()
                .location(location)
                .pickupDate(pickupDate)
                .dropoffDate(dropoffDate)
                .cars(cars)
                .isCached(false)
                .source("FALLBACK")
                .build();

        // 4. Save to cache
        try {
            String cacheKey = String.format("car|%s|%s|%s", location.toUpperCase(), pickupDate, dropoffDate);
            CarSearchCache cacheEntity = CarSearchCache.builder()
                    .searchKey(cacheKey)
                    .jsonResponse(objectMapper.writeValueAsString(response))
                    .createdAt(LocalDateTime.now())
                    .build();
            carCacheRepository.save(cacheEntity);
        } catch (Exception e) {
            log.warn("Could not cache car results", e);
        }

        return response;
    }
}
