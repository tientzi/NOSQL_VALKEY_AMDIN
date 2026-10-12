package com.groceryshop.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_method_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "method_key", nullable = false, unique = true, length = 50)
    private String methodKey; // COD, BANK_TRANSFER, MOMO

    @Column(nullable = false, length = 100)
    @Nationalized
    private String name;

    @Column(length = 500)
    @Nationalized
    private String description;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "bank_name", length = 100)
    @Nationalized
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder", length = 100)
    @Nationalized
    private String accountHolder;

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "transfer_syntax", length = 100)
    private String transferSyntax;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper compatibility methods
    @JsonProperty("methodCode")
    public String getMethodCode() {
        return methodKey;
    }

    @JsonProperty("methodName")
    public String getMethodName() {
        return name;
    }

    public String getCode() {
        return methodKey;
    }

    public void setCode(String code) {
        this.methodKey = code;
    }

    public String getQrImageUrl() {
        return qrCodeUrl;
    }

    public void setQrImageUrl(String qrImageUrl) {
        this.qrCodeUrl = qrImageUrl;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
        if (isEnabled == null) isEnabled = true;
    }
}
