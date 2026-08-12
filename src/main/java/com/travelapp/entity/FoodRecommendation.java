package com.travelapp.entity;

import com.travelapp.enums.FoodSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_recommendations", indexes = { @Index(name = "idx_foodrecommendation_destination", columnList = "destination_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String cuisine;

    private Double rating;

    private String priceRange;

    private String address;

    @Column(length = 500)
    private String zomatoUrl;

    @Column(length = 500)
    private String swiggyUrl;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FoodSource source = FoodSource.AI_RECOMMENDED;
}
