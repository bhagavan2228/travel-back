package com.travelapp.dto.car;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchResultDto {
    private String carId;
    private String brand;         // e.g. "Hertz", "Avis"
    private String vehicleName;   // e.g. "Toyota Corolla"
    private String type;          // e.g. "Compact", "SUV"
    private Integer seats;
    private String transmission;  // e.g. "Automatic", "Manual"
    private Double pricePerDay;
    private String priceFormatted; // e.g. "€45.00"
    private String currency;
    private String imageUrl;
}
