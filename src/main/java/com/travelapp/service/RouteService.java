package com.travelapp.service;

import com.travelapp.dto.route.RouteRequest;
import com.travelapp.dto.route.RouteResponse;
import com.travelapp.dto.route.RouteStep;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteService {

    public RouteResponse calculateRoute(RouteRequest request) {
        double distance = haversine(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestLat(), request.getDestLng());

        String mode = request.getMode() != null ? request.getMode() : "driving";
        int durationMinutes = switch (mode.toLowerCase()) {
            case "walking" -> (int) (distance / 5.0 * 60);
            case "cycling" -> (int) (distance / 15.0 * 60);
            case "transit" -> (int) (distance / 30.0 * 60);
            default -> (int) (distance / 50.0 * 60);
        };

        return RouteResponse.builder()
                .distanceKm(Math.round(distance * 10.0) / 10.0)
                .durationMinutes(Math.max(5, durationMinutes))
                .mode(mode)
                .steps(List.of(
                        RouteStep.builder()
                                .instruction("Head toward destination via main route")
                                .distanceKm(distance * 0.6)
                                .durationMinutes(durationMinutes * 2 / 3)
                                .build(),
                        RouteStep.builder()
                                .instruction("Arrive at destination")
                                .distanceKm(distance * 0.4)
                                .durationMinutes(durationMinutes / 3)
                                .build()
                ))
                .mockData(true)
                .build();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
