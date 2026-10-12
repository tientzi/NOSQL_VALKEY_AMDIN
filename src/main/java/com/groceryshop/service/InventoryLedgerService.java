package com.groceryshop.service;

import com.groceryshop.dto.InventoryLedgerDTO;
import com.groceryshop.entity.Inventory;
import com.groceryshop.entity.InventoryLedger;
import com.groceryshop.entity.User;
import com.groceryshop.repository.InventoryLedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryLedgerService {

    @Autowired
    private InventoryLedgerRepository ledgerRepository;

    @Transactional
    public void recordLog(Inventory inventory, String changeType, Integer quantityChange, Long referenceId, String note, User createdBy) {
        int previousStock = inventory.getCurrentStock() - quantityChange;
        
        InventoryLedger ledger = InventoryLedger.builder()
                .inventory(inventory)
                .changeType(changeType)
                .quantityChange(quantityChange)
                .previousStock(previousStock)
                .newStock(inventory.getCurrentStock())
                .referenceId(referenceId)
                .note(note)
                .createdBy(createdBy)
                .build();
                
        ledgerRepository.save(ledger);
    }

    public Page<InventoryLedgerDTO> getAllLedgers(Pageable pageable) {
        return ledgerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDTO);
    }

    public List<InventoryLedgerDTO> getLedgersByProduct(Long productId) {
        return ledgerRepository.findByInventoryProductIdOrderByCreatedAtDesc(productId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private InventoryLedgerDTO toDTO(InventoryLedger ledger) {
        return InventoryLedgerDTO.builder()
                .id(ledger.getId())
                .inventoryId(ledger.getInventory().getId())
                .productName(ledger.getInventory().getProduct().getName())
                .changeType(ledger.getChangeType())
                .quantityChange(ledger.getQuantityChange())
                .previousStock(ledger.getPreviousStock())
                .newStock(ledger.getNewStock())
                .referenceId(ledger.getReferenceId())
                .note(ledger.getNote())
                .createdBy(ledger.getCreatedBy() != null ? ledger.getCreatedBy().getUsername() : "Hệ thống")
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
