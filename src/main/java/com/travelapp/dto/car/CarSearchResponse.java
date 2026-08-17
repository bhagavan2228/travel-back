package com.travelapp.dto.car;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchResponse {
    private String location;
    private String pickupDate;
    private String dropoffDate;
    private List<CarSearchResultDto> cars;
    private boolean isCached;
    private String source;
}
