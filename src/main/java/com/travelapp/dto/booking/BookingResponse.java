package com.travelapp.dto.booking;

import com.travelapp.enums.BookingStatus;
import com.travelapp.enums.BookingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long tripId;
    private BookingType type;
    private BookingStatus status;
    private String provider;
    private String confirmationCode;
    private BigDecimal price;
    private String details;
    private LocalDateTime createdAt;
}
