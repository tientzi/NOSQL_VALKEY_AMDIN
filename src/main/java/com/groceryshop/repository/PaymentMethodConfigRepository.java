package com.groceryshop.repository;

import com.groceryshop.entity.PaymentMethodConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, Long> {
    Optional<PaymentMethodConfig> findByMethodKey(String methodKey);

    @Query("SELECT p FROM PaymentMethodConfig p WHERE p.methodKey = :code")
    Optional<PaymentMethodConfig> findByCode(@Param("code") String code);

    List<PaymentMethodConfig> findByIsEnabledTrue();
}

