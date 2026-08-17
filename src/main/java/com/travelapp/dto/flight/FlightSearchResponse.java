package com.travelapp.dto.flight;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResponse {
    private String origin;
    private String destination;
    private String date;
    private Integer passengers;
    private Integer totalResults;
    private Boolean isCached;
    private String source;
    private List<FlightSearchResultDto> flights;
}
