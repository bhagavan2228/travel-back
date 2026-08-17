package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flight_search_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", unique = true, nullable = false)
    private String cacheKey;

    @Column(name = "json_response", columnDefinition = "JSON", nullable = false)
    private String jsonResponse;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
