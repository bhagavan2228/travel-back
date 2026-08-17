package com.travelapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "car_search_cache")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchCache {

    @Id
    @Column(name = "search_key", length = 100)
    private String searchKey;

    @Column(columnDefinition = "LONGTEXT")
    private String jsonResponse;

    private LocalDateTime createdAt;
}
