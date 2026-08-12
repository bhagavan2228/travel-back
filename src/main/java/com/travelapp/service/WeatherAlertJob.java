package com.travelapp.service;

import com.travelapp.dto.weather.WeatherResponse;
import com.travelapp.entity.Notification;
import com.travelapp.entity.Trip;
import com.travelapp.enums.NotificationType;
import com.travelapp.enums.TripStatus;
import com.travelapp.repository.NotificationRepository;
import com.travelapp.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherAlertJob {

    private final TripRepository tripRepository;
    private final WeatherService weatherService;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 */6 * * *") // Every 6 hours
    @Transactional
    public void runWeatherAlertJob() {
        log.info("Starting weather alert scheduled job...");
        
        LocalDate now = LocalDate.now();
        List<Trip> activeTrips = tripRepository.findAll().stream()
                .filter(t -> t.getStatus() != TripStatus.COMPLETED && t.getStatus() != TripStatus.CANCELLED)
                .filter(t -> t.getStartDate() != null && !t.getStartDate().isAfter(now.plusDays(7)))
                .toList();

        for (Trip trip : activeTrips) {
            try {
                WeatherResponse weather = weatherService.getByDestination(trip.getDestination().getId());
                
                boolean alertNeeded = false;
                String alertMessage = "";
                
                // Check current weather
                if (weather.getTemperature() > 40) {
                    alertNeeded = true;
                    alertMessage = "Extreme heat alert for your trip to " + trip.getDestination().getName() + "! Temp: " + weather.getTemperature() + "°C";
                } else if (weather.getCondition().toLowerCase().contains("rain") || weather.getCondition().toLowerCase().contains("storm")) {
                    alertNeeded = true;
                    alertMessage = "Rain/Storm expected currently at " + trip.getDestination().getName() + ". Don't forget your umbrella!";
                }

                // Check forecast if trip is upcoming
                if (!alertNeeded && weather.getForecast() != null) {
                    for (var forecastDay : weather.getForecast()) {
                        if (trip.getStartDate() != null && !forecastDay.getDate().isBefore(trip.getStartDate())) {
                            if (forecastDay.getMaxTemp() > 40) {
                                alertNeeded = true;
                                alertMessage = "Extreme heat forecast on " + forecastDay.getDate() + " at " + trip.getDestination().getName() + "! Max Temp: " + forecastDay.getMaxTemp() + "°C";
                                break;
                            } else if (forecastDay.getCondition().toLowerCase().contains("rain") || forecastDay.getCondition().toLowerCase().contains("storm")) {
                                alertNeeded = true;
                                alertMessage = "Rain/Storm forecast on " + forecastDay.getDate() + " at " + trip.getDestination().getName() + ". Plan accordingly!";
                                break;
                            }
                        }
                    }
                }
                
                if (alertNeeded) {
                    notificationRepository.save(Notification.builder()
                            .user(trip.getUser())
                            .title("Weather Alert: " + trip.getDestination().getName())
                            .message(alertMessage)
                            .type(NotificationType.SYSTEM)
                            .read(false)
                            .build());
                    log.info("Generated weather alert for user {}: {}", trip.getUser().getId(), alertMessage);
                }
                
            } catch (Exception e) {
                log.error("Failed to process weather alert for trip {}: {}", trip.getId(), e.getMessage());
            }
        }
        
        log.info("Weather alert scheduled job completed.");
    }
}
