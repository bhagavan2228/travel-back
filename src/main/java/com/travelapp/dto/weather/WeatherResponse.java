package com.travelapp.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private String location;
    private Double temperature;
    private Double feelsLike;
    private Integer humidity;
    private String condition;
    private String description;
    private String icon;
    private Double windSpeed;
    private List<ForecastDay> forecast;
    private boolean mockData;
}
