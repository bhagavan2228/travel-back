package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "predicted_events", indexes = { @Index(name = "idx_predictedevent_destination", columnList = "destination_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private String title;

    private LocalDate eventDate;

    private String category;

    @Column(length = 2000)
    private String description;
}
