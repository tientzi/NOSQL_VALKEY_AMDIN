package com.groceryshop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType; // "CUSTOMER" hoặc "ADMIN"

    @Column(name = "sender_name", length = 100)
    @Nationalized
    private String senderName;

    @Column(nullable = false, length = 2000)
    @Nationalized
    private String message;

    @Column(name = "is_read_by_admin", nullable = false)
    @Builder.Default
    private Boolean isReadByAdmin = false;

    @Column(name = "is_read_by_customer", nullable = false)
    @Builder.Default
    private Boolean isReadByCustomer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isReadByAdmin == null) isReadByAdmin = false;
        if (isReadByCustomer == null) isReadByCustomer = false;
    }
}
