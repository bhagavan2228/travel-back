package com.travelapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credibility_scores", indexes = { @Index(name = "idx_credibilityscore_user", columnList = "user_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredibilityScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 100;

    @Builder.Default
    private Integer helpfulReviews = 0;

    @Builder.Default
    private Integer reportsResolved = 0;

    @Builder.Default
    private Integer totalReviews = 0;
}
