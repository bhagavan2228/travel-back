package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurants", indexes = {
        @Index(name = "idx_restaurant_destination", columnList = "destination_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private String name;

    private String cuisine;

    private Double rating;

    private Integer deliveryMinutes;

    private Integer costForTwo;

    private String imageUrl;

    @Column(nullable = false)
    private Integer rankOrder;
}
