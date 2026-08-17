package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_search_cache", indexes = {
    @Index(name = "idx_cache_key", columnList = "cache_key")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelSearchCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cache_key", nullable = false, unique = true, length = 255)
    private String cacheKey;

    @Lob
    @Column(name = "json_response", columnDefinition = "LONGTEXT")
    private String jsonResponse;

    private LocalDateTime createdAt;
}
