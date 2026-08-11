package com.travelapp.service;

import com.travelapp.config.AppProperties;
import com.travelapp.dto.weather.ForecastDay;
import com.travelapp.dto.weather.WeatherResponse;
import com.travelapp.entity.Destination;
import com.travelapp.entity.Trip;
import com.travelapp.entity.User;
import com.travelapp.exception.ApiException;
import com.travelapp.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final AppProperties appProperties;
    private final DestinationRepository destinationRepository;
    private final TripService tripService;
    private final WebClient.Builder webClientBuilder;

    @Transactional(readOnly = true)
    public WeatherResponse getByDestination(@NonNull Long destinationId) {
        Destination dest = destinationRepository.findById(destinationId)
                .orElseThrow(() -> ApiException.notFound("Destination not found"));
        return fetchWeather(dest.getName() + ", " + dest.getCountry(), dest.getLatitude(), dest.getLongitude());
    }

    @Transactional(readOnly = true)
    public WeatherResponse getByTrip(Long tripId, User user) {
        Trip trip = tripService.getUserTrip(tripId, user);
        Destination dest = trip.getDestination();
        return fetchWeather(dest.getName() + ", " + dest.getCountry(), dest.getLatitude(), dest.getLongitude());
    }

    private WeatherResponse fetchWeather(String location, Double lat, Double lng) {
        String apiKey = appProperties.getWeather().getApiKey();
        if (apiKey != null && !apiKey.isBlank() && lat != null && lng != null) {
            try {
                return fetchFromOpenWeather(location, lat, lng, apiKey);
            } catch (Exception ignored) {
                // fall through to mock
            }
        }
        return buildMockWeather(location);
    }

    private WeatherResponse fetchFromOpenWeather(String location, double lat, double lng, String apiKey) {
        String url = String.format("%s/weather?lat=%s&lon=%s&appid=%s&units=metric",
                appProperties.getWeather().getBaseUrl(), lat, lng, apiKey);

        var response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .block();

        if (response == null) {
            return buildMockWeather(location);
        }

        @SuppressWarnings("unchecked")
        var main = (java.util.Map<String, Object>) response.get("main");
        @SuppressWarnings("unchecked")
        var weatherList = (List<java.util.Map<String, Object>>) response.get("weather");
        var weather = weatherList != null && !weatherList.isEmpty() ? weatherList.get(0) : java.util.Map.of();

        return WeatherResponse.builder()
                .location(location)
                .temperature(((Number) main.get("temp")).doubleValue())
                .feelsLike(((Number) main.get("feels_like")).doubleValue())
                .humidity(((Number) main.get("humidity")).intValue())
                .condition((String) weather.get("main"))
                .description((String) weather.get("description"))
                .icon((String) weather.get("icon"))
                .windSpeed(5.0)
                .forecast(buildMockForecast())
                .mockData(false)
                .build();
    }

    private WeatherResponse buildMockWeather(String location) {
        Random rand = new Random(location.hashCode());
        return WeatherResponse.builder()
                .location(location)
                .temperature(18.0 + rand.nextInt(15))
                .feelsLike(17.0 + rand.nextInt(15))
                .humidity(50 + rand.nextInt(40))
                .condition("Partly Cloudy")
                .description("Pleasant weather for sightseeing")
                .icon("02d")
                .windSpeed(3.5 + rand.nextDouble() * 5)
                .forecast(buildMockForecast())
                .mockData(true)
                .build();
    }

    private List<ForecastDay> buildMockForecast() {
        List<ForecastDay> forecast = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            forecast.add(ForecastDay.builder()
                    .date(LocalDate.now().plusDays(i))
                    .minTemp(15.0 + i)
                    .maxTemp(22.0 + i)
                    .condition("Clear")
                    .description("Good travel conditions")
                    .build());
        }
        return forecast;
    }
}
