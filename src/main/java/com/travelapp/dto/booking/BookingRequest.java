package com.travelapp.dto.booking;

import com.travelapp.enums.BookingType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingRequest {
    @NotNull
    private Long tripId;
    @NotNull
    private BookingType type;
    private String provider;
    private BigDecimal price;
    private String details;
}
