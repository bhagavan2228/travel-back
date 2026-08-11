package com.travelapp.entity;

import com.travelapp.enums.FoodSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_recommendations")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FoodSource source = FoodSource.AI_RECOMMENDED;
}
