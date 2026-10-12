package com.groceryshop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "goods_receipt_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "import_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "batch_name", length = 50)
    private String batchName;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Quality Control (QC) Fields
    @Column(name = "passed_quantity")
    private Integer passedQuantity;

    @Column(name = "rejected_quantity")
    private Integer rejectedQuantity;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "qc_status", length = 50)
    private String qcStatus; // PENDING, PASSED, PARTIALLY_PASSED, REJECTED

    @Column(name = "qc_note", length = 500)
    private String qcNote;

    @Column(name = "inspected_at")
    private java.time.LocalDateTime inspectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by")
    private User inspectedBy;
}
