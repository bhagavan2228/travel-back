package com.travelapp.dto.flight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResultDto {
    private String offerId;           // Duffel offer ID
    private String airline;           // Airline name (e.g. "British Airways")
    private String flightNum;         // Operating carrier flight number
    private String departureTime;     // e.g. "2026-09-01T10:00:00"
    private String arrivalTime;       // e.g. "2026-09-01T14:30:00"
    private String duration;          // e.g. "7h 30m"
    private Double price;
    private String currency;
    private String priceFormatted;
    private String cabinClass;
    private Boolean cached;
    private String source;
}
