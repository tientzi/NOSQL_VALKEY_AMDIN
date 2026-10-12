package com.groceryshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Nullable: OAuth2 users do not have a local password
    @Column(length = 255)
    private String password;

    @Column(name = "full_name", length = 100)
    @Nationalized
    private String fullName;

    @Column(length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "loyalty_points", nullable = false)
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Column(name = "membership_tier", nullable = false, length = 50)
    @Builder.Default
    private String membershipTier = "BRONZE"; // BRONZE, SILVER, GOLD, DIAMOND

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // === OAuth2 fields ===
    // provider: LOCAL | GOOGLE | FACEBOOK
    @Column(length = 20)
    private String provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (loyaltyPoints == null) loyaltyPoints = 0;
        if (membershipTier == null) membershipTier = "BRONZE";
        if (provider == null) provider = "LOCAL";
    }
}
