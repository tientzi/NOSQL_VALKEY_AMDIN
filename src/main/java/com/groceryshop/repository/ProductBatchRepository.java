package com.groceryshop.repository;

import com.groceryshop.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    List<ProductBatch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(Long productId, int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM ProductBatch b WHERE b.product.id = :productId AND b.quantity > :quantity ORDER BY b.expiryDate ASC")
    List<ProductBatch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAscForUpdate(@Param("productId") Long productId, @Param("quantity") int quantity);
}
