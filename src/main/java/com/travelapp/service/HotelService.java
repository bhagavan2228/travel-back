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
                source = "SMART";
            }
        } else {
            log.info("Hotelbeds not configured — returning fallback hotels for {}", cCode);
            hotels = generateFallbackHotels(cCode, cIn, cOut, reqAdults);
            source = "SMART";
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

    // ═══════════════════════════════════════════════════════════════
    // DYNAMIC FALLBACK HOTEL GENERATOR (12–15 hotels)
    // ═══════════════════════════════════════════════════════════════

    private List<HotelSearchResultDto> generateFallbackHotels(String cityCode, String checkIn, String checkOut, int adults) {
        log.info("Generating dynamic fallback hotels for {} ({} to {})", cityCode, checkIn, checkOut);

        long seed = (cityCode + "|" + checkIn).hashCode();
        Random rng = new Random(seed);

        String city = cityCode.length() <= 3 ? getCityNameFromCode(cityCode) : cityCode;

        // --- Hotel name prefixes for different tiers ---
        String[] budgetPrefixes = {"FabHotel", "OYO", "Treebo", "Hotel Sagar", "Hotel Comfort"};
        String[] midPrefixes = {"Holiday Inn", "Novotel", "Radisson", "Lemon Tree", "Ibis"};
        String[] premiumPrefixes = {"Hyatt Regency", "Marriott", "Sheraton", "ITC", "Hilton"};
        String[] luxuryPrefixes = {"The Taj Palace", "The Oberoi", "Grand Hyatt", "The Leela", "JW Marriott"};

        // --- Room types by tier ---
        String[] budgetRooms = {"Standard Room", "Deluxe Room", "Twin Bed Room"};
        String[] midRooms = {"Superior Room", "Deluxe King Room", "Executive Room", "Club Room"};
        String[] premiumRooms = {"Premium Suite", "Executive Suite", "Deluxe King Suite", "Club Lounge Room"};
        String[] luxuryRooms = {"Presidential Suite", "Royal Suite", "Grand Luxury Suite", "Penthouse Suite"};

        // --- Board types ---
        String[] boardTypes = {"ROOM_ONLY", "BREAKFAST", "HALF_BOARD", "BREAKFAST"};

        // --- Amenities by tier ---
        String[][] budgetAmenities = {
                {"WiFi", "AC", "TV"},
                {"WiFi", "AC", "TV", "Room Service"},
                {"WiFi", "AC", "Parking"},
        };
        String[][] midAmenities = {
                {"WiFi", "AC", "TV", "Room Service", "Restaurant", "Parking"},
                {"WiFi", "AC", "Pool", "Gym", "Restaurant", "Room Service"},
                {"WiFi", "AC", "Gym", "Business Center", "Restaurant", "Parking"},
        };
        String[][] premiumAmenities = {
                {"WiFi", "AC", "Pool", "Spa", "Gym", "Restaurant", "Bar", "Room Service", "Concierge"},
                {"WiFi", "AC", "Pool", "Spa", "Gym", "Multiple Restaurants", "Business Center", "Valet Parking"},
                {"WiFi", "AC", "Pool", "Spa", "Gym", "Club Lounge", "Restaurant", "Laundry", "Airport Shuttle"},
        };
        String[][] luxuryAmenities = {
                {"WiFi", "AC", "Infinity Pool", "Luxury Spa", "Gym", "Fine Dining", "Rooftop Bar", "Butler Service", "Helipad", "Limousine"},
                {"WiFi", "AC", "Private Pool", "Spa & Wellness", "Gym", "Multiple Restaurants", "Private Beach", "Concierge", "Valet Parking"},
                {"WiFi", "AC", "Infinity Pool", "Award-Winning Spa", "Gym", "Michelin-Star Restaurant", "Champagne Bar", "Personal Butler"},
        };

        // --- Photo URLs by tier ---
        String[] budgetPhotos = {
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=800&q=80",
        };
        String[] midPhotos = {
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=800&q=80",
        };
        String[] premiumPhotos = {
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=800&q=80",
        };
        String[] luxuryPhotos = {
                "https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=800&q=80",
                "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=800&q=80",
        };

        // --- Generate 12-15 hotels across tiers ---
        int totalHotels = 12 + rng.nextInt(4); // 12 to 15
        // Distribution: ~3-4 budget, ~4-5 mid, ~3-4 premium, ~2-3 luxury
        int budgetCount = 3 + rng.nextInt(2);
        int midCount = 4 + rng.nextInt(2);
        int premiumCount = 3 + rng.nextInt(2);
        int luxuryCount = totalHotels - budgetCount - midCount - premiumCount;
        if (luxuryCount < 2) luxuryCount = 2;

        List<HotelSearchResultDto> hotels = new ArrayList<>();
        int hotelIdx = 0;

        // Base latitude/longitude from city code
        double[] cityCoords = getCityCoordinates(cityCode, rng);
        double baseLat = cityCoords[0];
        double baseLng = cityCoords[1];

        // --- Budget Hotels ---
        for (int i = 0; i < budgetCount; i++) {
            double price = 800 + rng.nextInt(1200); // ₹800 - ₹2000
            int stars = 2 + (rng.nextBoolean() ? 1 : 0); // 2-3 stars
            double rating = 3.2 + rng.nextDouble() * 1.0;
            int reviewCount = 100 + rng.nextInt(500);
            String hotelName = budgetPrefixes[rng.nextInt(budgetPrefixes.length)] + " " + city;
            if (i > 0) hotelName += " " + getLocationSuffix(rng);

            hotels.add(buildDynamicHotel(
                    "BDG" + String.format("%03d", hotelIdx++), hotelName, cityCode, city,
                    price, "INR", stars, rating, reviewCount,
                    budgetRooms[rng.nextInt(budgetRooms.length)],
                    boardTypes[rng.nextInt(2)], // Budget: mostly room only or breakfast
                    budgetAmenities[rng.nextInt(budgetAmenities.length)],
                    budgetPhotos[rng.nextInt(budgetPhotos.length)],
                    checkIn, checkOut, adults,
                    baseLat + (rng.nextDouble() - 0.5) * 0.05,
                    baseLng + (rng.nextDouble() - 0.5) * 0.05,
                    "Comfortable budget stay in " + city + " with essential amenities for travelers.",
                    rng
            ));
        }

        // --- Mid-range Hotels ---
        for (int i = 0; i < midCount; i++) {
            double price = 2000 + rng.nextInt(3000); // ₹2000 - ₹5000
            int stars = 3 + (rng.nextBoolean() ? 1 : 0); // 3-4 stars
            double rating = 3.8 + rng.nextDouble() * 0.8;
            int reviewCount = 200 + rng.nextInt(800);
            String hotelName = midPrefixes[rng.nextInt(midPrefixes.length)] + " " + city;
            if (i > 0) hotelName += " " + getLocationSuffix(rng);

            hotels.add(buildDynamicHotel(
                    "MID" + String.format("%03d", hotelIdx++), hotelName, cityCode, city,
                    price, "INR", stars, rating, reviewCount,
                    midRooms[rng.nextInt(midRooms.length)],
                    boardTypes[rng.nextInt(boardTypes.length)],
                    midAmenities[rng.nextInt(midAmenities.length)],
                    midPhotos[rng.nextInt(midPhotos.length)],
                    checkIn, checkOut, adults,
                    baseLat + (rng.nextDouble() - 0.5) * 0.04,
                    baseLng + (rng.nextDouble() - 0.5) * 0.04,
                    "Well-appointed mid-range hotel in " + city + " offering modern comfort and excellent hospitality.",
                    rng
            ));
        }

        // --- Premium Hotels ---
        for (int i = 0; i < premiumCount; i++) {
            double price = 5000 + rng.nextInt(5000); // ₹5000 - ₹10000
            int stars = 4 + (rng.nextBoolean() ? 1 : 0); // 4-5 stars
            double rating = 4.2 + rng.nextDouble() * 0.6;
            int reviewCount = 500 + rng.nextInt(1500);
            String hotelName = premiumPrefixes[rng.nextInt(premiumPrefixes.length)] + " " + city;
            if (i > 0) hotelName += " " + getLocationSuffix(rng);

            hotels.add(buildDynamicHotel(
                    "PRM" + String.format("%03d", hotelIdx++), hotelName, cityCode, city,
                    price, "INR", stars, rating, reviewCount,
                    premiumRooms[rng.nextInt(premiumRooms.length)],
                    boardTypes[1 + rng.nextInt(boardTypes.length - 1)], // Premium: breakfast or half board
                    premiumAmenities[rng.nextInt(premiumAmenities.length)],
                    premiumPhotos[rng.nextInt(premiumPhotos.length)],
                    checkIn, checkOut, adults,
                    baseLat + (rng.nextDouble() - 0.5) * 0.03,
                    baseLng + (rng.nextDouble() - 0.5) * 0.03,
                    "Premium luxury hotel in " + city + " with world-class service and spectacular views.",
                    rng
            ));
        }

        // --- Luxury Hotels ---
        for (int i = 0; i < luxuryCount; i++) {
            double price = 10000 + rng.nextInt(15000); // ₹10000 - ₹25000
            int stars = 5;
            double rating = 4.5 + rng.nextDouble() * 0.4;
            int reviewCount = 1000 + rng.nextInt(3000);
            String hotelName = luxuryPrefixes[rng.nextInt(luxuryPrefixes.length)] + " " + city;
            if (i > 0) hotelName += " " + getLocationSuffix(rng);

            hotels.add(buildDynamicHotel(
                    "LUX" + String.format("%03d", hotelIdx++), hotelName, cityCode, city,
                    price, "INR", stars, rating, reviewCount,
                    luxuryRooms[rng.nextInt(luxuryRooms.length)],
                    boardTypes[1 + rng.nextInt(boardTypes.length - 1)], // Luxury: breakfast or half board
                    luxuryAmenities[rng.nextInt(luxuryAmenities.length)],
                    luxuryPhotos[rng.nextInt(luxuryPhotos.length)],
                    checkIn, checkOut, adults,
                    baseLat + (rng.nextDouble() - 0.5) * 0.02,
                    baseLng + (rng.nextDouble() - 0.5) * 0.02,
                    "Ultra-luxury 5-star experience in " + city + " offering unparalleled elegance, gourmet dining, and bespoke services.",
                    rng
            ));
        }

        return hotels;
    }

    private HotelSearchResultDto buildDynamicHotel(String hotelId, String name, String cityCode,
                                                     String city, double price, String currency,
                                                     int starRating, double rating, int reviewCount,
                                                     String roomType, String boardType,
                                                     String[] amenities, String photoUrl,
                                                     String checkIn, String checkOut, int adults,
                                                     double lat, double lng, String description,
                                                     Random rng) {
        rating = Math.round(rating * 10.0) / 10.0;
        int priceLevel = starRating <= 2 ? 1 : starRating <= 3 ? 2 : starRating <= 4 ? 3 : 4;
        String priceLevelStr = "₹".repeat(priceLevel);

        int addressNum = 1 + rng.nextInt(200);
        String[] streets = {"Main Road", "Station Road", "MG Road", "Park Street", "Lake View Road",
                "Ring Road", "Beach Road", "Hill Road", "Temple Street", "Market Road"};
        String address = addressNum + " " + streets[rng.nextInt(streets.length)] + ", " + city;

        return HotelSearchResultDto.builder()
                .hotelId(hotelId)
                .name(name)
                .address(address)
                .cityCode(cityCode)
                .latitude(lat)
                .longitude(lng)
                .offerId("SMART-" + UUID.randomUUID().toString().substring(0, 8))
                .currency(currency)
                .price(price)
                .priceFormatted(formatPrice(currency, price))
                .roomType(roomType)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .adults(adults)
                .boardType(boardType)
                .rating(rating)
                .userRatingCount(reviewCount)
                .priceLevel(priceLevel)
                .priceLevelString(priceLevelStr)
                .photoUrl(photoUrl)
                .luxuryScore(rating * 0.4 + starRating * 0.6)
                .starRating(starRating)
                .amenities(List.of(amenities))
                .description(description)
                .cached(false)
                .source("SMART")
                .build();
    }

    private String getLocationSuffix(Random rng) {
        String[] suffixes = {"City Centre", "Airport Road", "Business Park", "Riverside",
                "Old Town", "Lake View", "Station Area", "Heritage Quarter",
                "Tech Park", "Golf Course Road"};
        return suffixes[rng.nextInt(suffixes.length)];
    }

    private String getCityNameFromCode(String code) {
        return switch (code.toUpperCase()) {
            case "MUM", "BOM" -> "Mumbai";
            case "DEL", "NDL" -> "Delhi";
            case "BLR", "BNG" -> "Bangalore";
            case "MAA", "CHN" -> "Chennai";
            case "HYD" -> "Hyderabad";
            case "CCU", "KOL" -> "Kolkata";
            case "GOI" -> "Goa";
            case "JAI" -> "Jaipur";
            case "PNQ" -> "Pune";
            case "AMD" -> "Ahmedabad";
            case "COK" -> "Kochi";
            case "TRV" -> "Thiruvananthapuram";
            case "LKO" -> "Lucknow";
            case "PAT" -> "Patna";
            case "IXC" -> "Chandigarh";
            case "PAR" -> "Paris";
            case "LON", "LHR" -> "London";
            case "NYC", "JFK" -> "New York";
            case "TYO", "NRT" -> "Tokyo";
            case "SIN" -> "Singapore";
            case "DXB" -> "Dubai";
            case "BKK" -> "Bangkok";
            case "SYD" -> "Sydney";
            case "ROM", "FCO" -> "Rome";
            case "BCN" -> "Barcelona";
            default -> code;
        };
    }

    private double[] getCityCoordinates(String code, Random rng) {
        return switch (code.toUpperCase()) {
            case "MUM", "BOM" -> new double[]{19.0760, 72.8777};
            case "DEL", "NDL" -> new double[]{28.6139, 77.2090};
            case "BLR", "BNG" -> new double[]{12.9716, 77.5946};
            case "MAA", "CHN" -> new double[]{13.0827, 80.2707};
            case "HYD" -> new double[]{17.3850, 78.4867};
            case "CCU", "KOL" -> new double[]{22.5726, 88.3639};
            case "GOI" -> new double[]{15.2993, 74.1240};
            case "JAI" -> new double[]{26.9124, 75.7873};
            case "PNQ" -> new double[]{18.5204, 73.8567};
            case "AMD" -> new double[]{23.0225, 72.5714};
            case "COK" -> new double[]{9.9312, 76.2673};
            case "TRV" -> new double[]{8.5241, 76.9366};
            case "PAR" -> new double[]{48.8566, 2.3522};
            case "LON", "LHR" -> new double[]{51.5074, -0.1278};
            case "NYC", "JFK" -> new double[]{40.7128, -74.0060};
            case "TYO", "NRT" -> new double[]{35.6762, 139.6503};
            case "SIN" -> new double[]{1.3521, 103.8198};
            case "DXB" -> new double[]{25.2048, 55.2708};
            case "BKK" -> new double[]{13.7563, 100.5018};
            case "SYD" -> new double[]{-33.8688, 151.2093};
            default -> new double[]{20.0 + rng.nextDouble() * 10, 75.0 + rng.nextDouble() * 10};
        };
    }
}
