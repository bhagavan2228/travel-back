package com.travelapp.dto.trip;

import com.travelapp.dto.destination.DestinationResponse;
import com.travelapp.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {
    private Long id;
    private String title;
    private Long userId;
    private DestinationResponse destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private TripStatus status;
    private String notes;
    private Integer travelers;
}
