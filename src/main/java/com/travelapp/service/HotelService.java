package com.travelapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelapp.dto.hotel.HotelSearchResultDto;
import com.travelapp.dto.hotel.HotelSearchResponse;
import com.travelapp.entity.HotelSearchCache;
import com.travelapp.repository.HotelSearchCacheRepository;
import com.travelapp.service.integration.HotelbedsApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelService {

    private final HotelbedsApiClient hotelbedsApiClient;
    private final HotelSearchCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public HotelSearchResponse searchHotels(String cityCode, String checkIn, String checkOut,
                                             Integer adults, String sortBy) {
        // --- Defaults ---
        String cCode = (cityCode != null && !cityCode.isBlank()) ? cityCode.trim().toUpperCase() : "PAR";
        String cIn  = (checkIn  != null && !checkIn.isBlank())  ? checkIn.trim()  : LocalDate.now().plusDays(7).format(DATE_FMT);
        String cOut = (checkOut != null && !checkOut.isBlank()) ? checkOut.trim() : LocalDate.now().plusDays(10).format(DATE_FMT);
        int reqAdults = (adults != null && adults > 0) ? adults : 1;
        String reqSort = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "price";

        String cacheKey = String.format("hotel|%s|%s|%s|%d", cCode, cIn, cOut, reqAdults);

        // 1. Check MySQL cache (1-hour TTL)
        Optional<HotelSearchCache> cached = cacheRepository.findByCacheKey(cacheKey);
        if (cached.isPresent() && cached.get().getCreatedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            log.info("Returning cached hotel results for key: {}", cacheKey);
            try {
                List<HotelSearchResultDto> list = objectMapper.readValue(
                        cached.get().getJsonResponse(),
                        new TypeReference<List<HotelSearchResultDto>>() {});
                list.forEach(h -> h.setCached(true));
                return buildResponse(cCode, cIn, cOut, reqAdults, reqSort, list, true, "HOTELBEDS");
            } catch (Exception e) {
                log.warn("Cache parse failed for key {}, fetching fresh", cacheKey, e);
            }
        }

        // 2. Fetch from Hotelbeds
        List<HotelSearchResultDto> hotels;
        String source;

        if (hotelbedsApiClient.isConfigured()) {
            hotels = fetchFromHotelbeds(cCode, cIn, cOut, reqAdults);
            source = "HOTELBEDS";
            if (hotels.isEmpty()) {
                log.warn("Hotelbeds returned 0 results for {}, using fallback", cCode);
                hotels = generateFallbackHotels(cCode, cIn, cOut, reqAdults);
                source = "FALLBACK";
            }
        } else {
            log.info("Hotelbeds not configured — returning fallback hotels for {}", cCode);
            hotels = generateFallbackHotels(cCode, cIn, cOut, reqAdults);
            source = "FALLBACK";
        }

        // 3. Save to MySQL cache
        try {
            String json = objectMapper.writeValueAsString(hotels);
            HotelSearchCache entry = cached.orElseGet(() -> HotelSearchCache.builder().cacheKey(cacheKey).build());
            entry.setJsonResponse(json);
            entry.setCreatedAt(LocalDateTime.now());
            cacheRepository.save(entry);
            log.info("Cached {} hotels for key: {}", hotels.size(), cacheKey);
        } catch (Exception e) {
            log.error("Failed to cache hotel results", e);
        }

        return buildResponse(cCode, cIn, cOut, reqAdults, reqSort, hotels, false, source);
    }

    private List<HotelSearchResultDto> fetchFromHotelbeds(String cityCode, String checkIn, String checkOut, int adults) {
        List<HotelSearchResultDto> results = new ArrayList<>();
        try {
            Map<String, Object> body = new HashMap<>();
            
            Map<String, String> stay = new HashMap<>();
            stay.put("checkIn", checkIn);
            stay.put("checkOut", checkOut);
            body.put("stay", stay);

            List<Map<String, Object>> occupancies = new ArrayList<>();
            Map<String, Object> occ = new HashMap<>();
            occ.put("rooms", 1);
            occ.put("adults", adults);
            occ.put("children", 0);
            occupancies.add(occ);
            body.put("occupancies", occupancies);

            Map<String, String> destination = new HashMap<>();
            destination.put("code", cityCode);
            body.put("destination", destination);

            log.info("Calling Hotelbeds API for destination {}", cityCode);
            String response = hotelbedsApiClient.authenticatedPost("/hotels", body);
            if (response == null) return results;

            JsonNode root = objectMapper.readTree(response);

            if (root.has("error")) {
                log.error("Hotelbeds API error: {}", root.path("error"));
                return results;
            }

            JsonNode hotels = root.path("hotels").path("hotels");
            if (hotels.isArray()) {
                for (JsonNode hotelNode : hotels) {
                    HotelSearchResultDto dto = parseHotelbedsHotel(hotelNode, cityCode, checkIn, checkOut, adults);
                    if (dto != null) {
                        results.add(dto);
                    }
                }
            }
            log.info("Hotelbeds API returned {} offers", results.size());
        } catch (Exception e) {
            log.error("Error calling Hotelbeds API: {}", e.getMessage());
        }
        return results;
    }

    private HotelSearchResultDto parseHotelbedsHotel(JsonNode hotelNode, String cityCode,
                                                  String checkIn, String checkOut, int adults) {
        try {
            String hotelId = hotelNode.path("code").asText("unknown");
            String name = hotelNode.path("name").asText("Hotel");
            Double lat = hotelNode.has("latitude") ? hotelNode.path("latitude").asDouble() : null;
            Double lng = hotelNode.has("longitude") ? hotelNode.path("longitude").asDouble() : null;
            String address = hotelNode.path("destinationName").asText("") + ", " + hotelNode.path("zoneName").asText("");

            String currency = hotelNode.path("currency").asText("USD");
            Double minRate = hotelNode.path("minRate").asDouble(0.0);

            JsonNode rooms = hotelNode.path("rooms");
            String offerId = null;
            String roomType = null;
            String boardType = null;

            if (rooms.isArray() && !rooms.isEmpty()) {
                JsonNode room = rooms.get(0);
                roomType = room.path("name").asText("Standard Room");

                JsonNode rates = room.path("rates");
                if (rates.isArray() && !rates.isEmpty()) {
                    JsonNode rate = rates.get(0);
                    offerId = rate.path("rateKey").asText(null);
                    boardType = rate.path("boardName").asText("ROOM ONLY");
                }
            }

            String priceFormatted = formatPrice(currency, minRate);

            return HotelSearchResultDto.builder()
                    .hotelId(hotelId)
                    .name(name)
                    .address(address)
                    .cityCode(cityCode)
                    .latitude(lat)
                    .longitude(lng)
                    .offerId(offerId)
                    .currency(currency)
                    .price(minRate)
                    .priceFormatted(priceFormatted)
                    .roomType(roomType != null ? roomType : "Standard Room")
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .adults(adults)
                    .boardType(boardType != null ? boardType : "ROOM_ONLY")
                    .rating(null)
                    .userRatingCount(null)
                    .priceLevel(null)
                    .priceLevelString(null)
                    .photoUrl(null)
                    .luxuryScore(null)
                    .cached(false)
                    .source("HOTELBEDS")
                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse Hotelbeds hotel offer: {}", e.getMessage());
            return null;
        }
    }

    private List<HotelSearchResultDto> applySorting(List<HotelSearchResultDto> list, String sortBy) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<HotelSearchResultDto> sorted = new ArrayList<>(list);
        switch (sortBy) {
            case "price":
            case "price_asc":
                sorted.sort(Comparator.comparing(HotelSearchResultDto::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "price_desc":
                sorted.sort(Comparator.comparing(HotelSearchResultDto::getPrice, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "rating":
                sorted.sort(Comparator.comparing(HotelSearchResultDto::getRating, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            case "luxury":
                sorted.sort(Comparator.comparing(HotelSearchResultDto::getLuxuryScore, Comparator.nullsLast(Comparator.reverseOrder())));
                break;
            default:
                sorted.sort(Comparator.comparing(HotelSearchResultDto::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
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

    private HotelSearchResponse buildResponse(String cityCode, String checkIn, String checkOut,
                                                int adults, String sortBy,
                                                List<HotelSearchResultDto> hotels,
                                                boolean isCached, String source) {
        List<HotelSearchResultDto> sorted = applySorting(hotels, sortBy);
        return HotelSearchResponse.builder()
                .cityCode(cityCode)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .adults(adults)
                .sortBy(sortBy)
                .totalResults(sorted.size())
                .isCached(isCached)
                .source(source)
                .hotels(sorted)
                .build();
    }

    private List<HotelSearchResultDto> generateFallbackHotels(String cityCode, String checkIn, String checkOut, int adults) {
        String city = cityCode;
        log.info("Generating fallback hotels for {} ({})", city, cityCode);
        return List.of(
                buildFallback("DEMO001", "Grand Palace Hotel " + city,
                        "1 Central Avenue, " + city, cityCode, 285.00, "USD", "Deluxe King Room", checkIn, checkOut, adults, 48.8566, 2.3522),
                buildFallback("DEMO002", "Boutique Heritage Inn " + city,
                        "45 Historic Lane, " + city, cityCode, 189.50, "USD", "Heritage Suite", checkIn, checkOut, adults, 48.8584, 2.3488),
                buildFallback("DEMO003", city + " City Center Hotel",
                        "12 Station Road, " + city, cityCode, 125.00, "USD", "Standard Double", checkIn, checkOut, adults, 48.8600, 2.3500)
        );
    }

    private HotelSearchResultDto buildFallback(String id, String name, String address, String cityCode,
                                                double price, String currency, String roomType,
                                                String checkIn, String checkOut, int adults,
                                                double lat, double lng) {
        return HotelSearchResultDto.builder()
                .hotelId(id)
                .name(name)
                .address(address)
                .cityCode(cityCode)
                .latitude(lat)
                .longitude(lng)
                .offerId("DEMO-" + UUID.randomUUID().toString().substring(0, 8))
                .currency(currency)
                .price(price)
                .priceFormatted(formatPrice(currency, price))
                .roomType(roomType)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .adults(adults)
                .boardType("ROOM_ONLY")
                .photoUrl("https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80")
                .cached(false)
                .source("FALLBACK")
                .build();
    }
}
