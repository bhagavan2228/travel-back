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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id")
    private Destination destination;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String googlePlaceId;

    private String cuisine; // from places.primaryTypeDisplayName

    private Double rating;

    private Integer userRatingsTotal;

    @Column(length = 500)
    private String address;

    private Double latitude;

    private Double longitude;

    private String priceLevel;

    private String website;

    @Column(length = 500)
    private String googleMapsUri;

    private String businessStatus;

    @Column(length = 2000)
    private String imageUrl;

    @Column(nullable = false)
    private Integer rankOrder;
}
