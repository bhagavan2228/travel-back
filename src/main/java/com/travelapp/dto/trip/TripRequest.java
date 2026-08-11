package com.travelapp.dto.trip;

import com.travelapp.enums.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TripRequest {
    private String title;
    @NotNull
    private Long destinationId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private TripStatus status;
    private String notes;
    private Integer travelers;
}
