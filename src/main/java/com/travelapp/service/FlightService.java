package com.travelapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.dto.flight.FlightSearchResultDto;
import com.travelapp.dto.flight.FlightSearchResponse;
import com.travelapp.entity.FlightSearchCache;
import com.travelapp.repository.FlightSearchCacheRepository;
import com.travelapp.service.integration.DuffelApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlightService {

    private final DuffelApiClient duffelApiClient;
    private final FlightSearchCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public FlightSearchResponse searchFlights(String origin, String destination, String date,
                                             Integer passengers, String sortBy) {
        // --- Defaults ---
        String oCode = (origin != null && !origin.isBlank()) ? origin.trim().toUpperCase() : "LHR";
        String dCode = (destination != null && !destination.isBlank()) ? destination.trim().toUpperCase() : "JFK";
        String dDate  = (date  != null && !date.isBlank())  ? date.trim()  : LocalDate.now().plusDays(14).format(DATE_FMT);
        int reqPassengers = (passengers != null && passengers > 0) ? passengers : 1;
        String reqSort = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "price";

        String cacheKey = String.format("flight|%s|%s|%s|%d", oCode, dCode, dDate, reqPassengers);

        // 1. Check MySQL cache (1-hour TTL)
        Optional<FlightSearchCache> cached = cacheRepository.findByCacheKey(cacheKey);
        if (cached.isPresent() && cached.get().getCreatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            log.info("Returning cached flight results for key: {}", cacheKey);
            try {
                List<FlightSearchResultDto> list = objectMapper.readValue(
                        cached.get().getJsonResponse(),
                        new TypeReference<List<FlightSearchResultDto>>() {});
                list.forEach(f -> f.setCached(true));
                return buildResponse(oCode, dCode, dDate, reqPassengers, reqSort, list, true, "DUFFEL");
            } catch (Exception e) {
                log.warn("Cache parse failed for key {}, fetching fresh", cacheKey, e);
            }
        }

        // 2. Fetch from Duffel
        List<FlightSearchResultDto> flights;
        String source;

        if (duffelApiClient.isConfigured()) {
            flights = fetchFromDuffel(oCode, dCode, dDate, reqPassengers);
            source = "DUFFEL";
            if (flights.isEmpty()) {
                log.warn("Duffel returned 0 results for {}, using fallback", cacheKey);
                flights = generateFallbackFlights(oCode, dCode, dDate);
                source = "FALLBACK";
            }
        } else {
            log.info("Duffel not configured — returning fallback flights for {}", cacheKey);
            flights = generateFallbackFlights(oCode, dCode, dDate);
            source = "FALLBACK";
        }

        // 3. Save to MySQL cache
        try {
            String json = objectMapper.writeValueAsString(flights);
            FlightSearchCache entry = cached.orElseGet(() -> FlightSearchCache.builder().cacheKey(cacheKey).build());
            entry.setJsonResponse(json);
            entry.setCreatedAt(LocalDateTime.now());
            cacheRepository.save(entry);
            log.info("Cached {} flights for key: {}", flights.size(), cacheKey);
        } catch (Exception e) {
            log.error("Failed to cache flight results", e);
        }

        return buildResponse(oCode, dCode, dDate, reqPassengers, reqSort, flights, false, source);
    }

    private List<FlightSearchResultDto> fetchFromDuffel(String origin, String destination, String date, int passengers) {
        List<FlightSearchResultDto> results = new ArrayList<>();
        try {
            // Build Duffel Offer Request body
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> data = new HashMap<>();

            List<Map<String, String>> slices = new ArrayList<>();
            Map<String, String> slice = new HashMap<>();
            slice.put("origin", origin);
            slice.put("destination", destination);
            slice.put("departure_date", date);
            slices.add(slice);
            data.put("slices", slices);

            List<Map<String, String>> passList = new ArrayList<>();
            for (int i = 0; i < passengers; i++) {
                Map<String, String> pass = new HashMap<>();
                pass.put("type", "adult");
                passList.add(pass);
            }
            data.put("passengers", passList);
            data.put("cabin_class", "economy");
            body.put("data", data);

            log.info("Calling Duffel API to create offer request for {} to {} on {}", origin, destination, date);
            String response = duffelApiClient.authenticatedPost("/air/offer_requests", body);
            if (response == null) return results;

            JsonNode root = objectMapper.readTree(response);
            if (root.has("errors")) {
                log.error("Duffel API error: {}", root.path("errors"));
                return results;
            }

            JsonNode offers = root.path("data").path("offers");
            if (offers.isArray()) {
                for (JsonNode offer : offers) {
                    FlightSearchResultDto dto = parseDuffelOffer(offer);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
            }
            log.info("Duffel API returned {} offers", results.size());
        } catch (Exception e) {
            log.error("Error calling Duffel API: {}", e.getMessage());
        }
        return results;
    }

    private FlightSearchResultDto parseDuffelOffer(JsonNode offer) {
        try {
            String offerId = offer.path("id").asText();
            String airline = offer.path("owner").path("name").asText("Unknown Airline");
            Double price = offer.path("total_amount").asDouble(0.0);
            String currency = offer.path("total_currency").asText("USD");

            JsonNode slices = offer.path("slices");
            String flightNum = "N/A";
            String depTime = "N/A";
            String arrTime = "N/A";
            String durationStr = "N/A";

            if (slices.isArray() && !slices.isEmpty()) {
                JsonNode slice = slices.get(0);
                durationStr = slice.path("duration").asText();
                JsonNode segments = slice.path("segments");
                if (segments.isArray() && !segments.isEmpty()) {
                    JsonNode seg = segments.get(0);
                    depTime = seg.path("departing_at").asText();
                    arrTime = seg.path("arriving_at").asText();
                    flightNum = seg.path("operating_carrier").path("iata_code").asText("") +
                                seg.path("operating_carrier_flight_number").asText("");
                }
            }

            return FlightSearchResultDto.builder()
                    .offerId(offerId)
                    .airline(airline)
                    .flightNum(flightNum)
                    .departureTime(depTime)
                    .arrivalTime(arrTime)
                    .duration(durationStr)
                    .price(price)
                    .currency(currency)
                    .priceFormatted(formatPrice(currency, price))
                    .cabinClass("economy")
                    .cached(false)
                    .source("DUFFEL")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Duffel offer: {}", e.getMessage());
            return null;
        }
    }

    private List<FlightSearchResultDto> applySorting(List<FlightSearchResultDto> list, String sortBy) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<FlightSearchResultDto> sorted = new ArrayList<>(list);
        switch (sortBy) {
            case "price":
            case "price_asc":
                sorted.sort(Comparator.comparing(FlightSearchResultDto::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "price_desc":
                sorted.sort(Comparator.comparing(FlightSearchResultDto::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            default:
                sorted.sort(Comparator.comparing(FlightSearchResultDto::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
        }
        return sorted;
    }

    private String formatPrice(String currency, Double price) {
        if (price == null) return "N/A";
        String symbol = switch (currency) {
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "INR" -> "₹";
            case "JPY" -> "¥";
            default -> "$";
        };
        return symbol + String.format("%.2f", price);
    }

    private FlightSearchResponse buildResponse(String origin, String dest, String date,
                                                int pass, String sortBy,
                                                List<FlightSearchResultDto> flights,
                                                boolean isCached, String source) {
        List<FlightSearchResultDto> sorted = applySorting(flights, sortBy);
        return FlightSearchResponse.builder()
                .origin(origin)
                .destination(dest)
                .date(date)
                .passengers(pass)
                .totalResults(sorted.size())
                .isCached(isCached)
                .source(source)
                .flights(sorted)
                .build();
    }

    private List<FlightSearchResultDto> generateFallbackFlights(String origin, String dest, String date) {
        return List.of(
                buildFallback("DEMO_FL1", "TravelAir", "TA101", date + "T08:00:00", date + "T10:30:00", "2h 30m", 150.00, "USD"),
                buildFallback("DEMO_FL2", "SkyHigh Airlines", "SH202", date + "T14:15:00", date + "T16:45:00", "2h 30m", 220.00, "USD"),
                buildFallback("DEMO_FL3", "BudgetJet", "BJ303", date + "T20:00:00", date + "T22:45:00", "2h 45m", 95.00, "USD")
        );
    }

    private FlightSearchResultDto buildFallback(String id, String airline, String flightNum,
                                                String dep, String arr, String dur, double price, String currency) {
        return FlightSearchResultDto.builder()
                .offerId(id)
                .airline(airline)
                .flightNum(flightNum)
                .departureTime(dep)
                .arrivalTime(arr)
                .duration(dur)
                .price(price)
                .currency(currency)
                .priceFormatted(formatPrice(currency, price))
                .cabinClass("economy")
                .cached(false)
                .source("FALLBACK")
                .build();
    }
}
