package com.travelapp.dto.hotel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchResultDto {
    // --- Hotelbeds data ---
    private String hotelId;           // Hotelbeds hotel code
    private String name;              // Hotel name
    private String address;           // Formatted address
    private String cityCode;          // Destination code
    private Double latitude;
    private Double longitude;

    // Hotelbeds offer data
    private String offerId;           // Hotelbeds rateKey
    private String currency;          // e.g. "EUR"
    private Double price;             // Live price from Hotelbeds
    private String priceFormatted;    // e.g. "€245.00"
    private String roomType;          // Room description
    private String checkIn;
    private String checkOut;
    private Integer adults;
    private String boardType;         // e.g. "ROOM_ONLY", "BREAKFAST"

    // --- Google Places enrichment (Phase 2 — null until then) ---
    private Double rating;            // Google star rating
    private Integer userRatingCount;  // Google review count
    private Integer priceLevel;       // Google normalized 1-4 scale
    private String priceLevelString;  // "₹" to "₹₹₹₹"
    private String photoUrl;          // Google Places photo URL

    // --- Computed (Phase 3) ---
    private Double luxuryScore;       // Weighted combo

    // --- Extended fields ---
    private Integer starRating;       // Hotel star rating (2-5)
    private List<String> amenities;   // e.g. ["WiFi", "Pool", "Spa"]
    private String description;       // Brief hotel description

    private Boolean cached;           // Whether result came from MySQL cache
    private String source;            // "HOTELBEDS", "HOTELBEDS+GOOGLE", "FALLBACK"
}

