package com.groceryshop.repository;

import com.groceryshop.entity.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    Page<GoodsReceipt> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsBySupplierId(Long supplierId);
}
