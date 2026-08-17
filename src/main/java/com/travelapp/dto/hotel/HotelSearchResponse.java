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
public class HotelSearchResponse {
    private String cityCode;
    private String checkIn;
    private String checkOut;
    private Integer adults;
    private String sortBy;
    private Integer totalResults;
    private Boolean isCached;
    private String source;          // "AMADEUS", "FALLBACK"
    private List<HotelSearchResultDto> hotels;
}
