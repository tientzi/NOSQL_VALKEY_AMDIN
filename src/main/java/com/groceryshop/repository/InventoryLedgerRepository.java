package com.groceryshop.repository;

import com.groceryshop.entity.InventoryLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {
    List<InventoryLedger> findByInventoryProductIdOrderByCreatedAtDesc(Long productId);
    Page<InventoryLedger> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
