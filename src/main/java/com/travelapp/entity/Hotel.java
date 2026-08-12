package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "hotels", indexes = { @Index(name = "idx_hotel_destination", columnList = "destination_id") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    private String name;
    
    @Column(length = 1000)
    private String imageUrl;
    
    private Double rating;
    private Integer price;
    private Integer vacancies;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_reviews", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "review", length = 1000)
    private List<String> reviews;
}
