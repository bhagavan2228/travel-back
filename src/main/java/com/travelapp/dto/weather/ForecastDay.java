package com.travelapp.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDay {
    private LocalDate date;
    private Double minTemp;
    private Double maxTemp;
    private String condition;
    private String description;
}
