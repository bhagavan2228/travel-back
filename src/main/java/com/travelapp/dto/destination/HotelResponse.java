package com.travelapp.dto.destination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private Double rating;
    private Integer price;
    private Integer vacancies;
    private List<String> reviews;
}
