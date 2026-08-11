package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_items", indexes = {
        @Index(name = "idx_menu_restaurant", columnList = "restaurant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    private Double rating;

    @Column(nullable = false)
    @Builder.Default
    private boolean veg = false;

    private String category;

    private String imageUrl;

    @Column(nullable = false)
    private Integer sortOrder;
}
