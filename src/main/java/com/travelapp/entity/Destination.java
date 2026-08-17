package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    private String city;

    private String state;

    @Column(length = 5000)
    private String description;

    @Column(length = 1024)
    private String imageUrl;

    private Double latitude;

    private Double longitude;

    private String climate;

    private String bestSeason;

    @Column(length = 500)
    private String tags;

    @Builder.Default
    private Integer exploredCount = 0;
}
