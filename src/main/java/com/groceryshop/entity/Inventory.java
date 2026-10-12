package com.groceryshop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Check(constraints = "current_stock >= 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", unique = true)
    private Product product;

    @Min(value = 0, message = "Tồn kho không được âm")
    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock;

    @Column(length = 100)
    @Nationalized
    private String location;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        if (currentStock == null) currentStock = 0;
        if (minimumStock == null) minimumStock = 5;
    }
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        if (currentStock == null) currentStock = 0;
        if (minimumStock == null) minimumStock = 5;
    }
}
