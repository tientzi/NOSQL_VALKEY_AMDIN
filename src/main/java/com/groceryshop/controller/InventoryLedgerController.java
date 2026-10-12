package com.groceryshop.controller;

import com.groceryshop.dto.InventoryLedgerDTO;
import com.groceryshop.service.InventoryLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory-ledger")
@CrossOrigin(origins = "*")
public class InventoryLedgerController {

    @Autowired
    private InventoryLedgerService ledgerService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InventoryLedgerDTO>> getAllLedgers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ledgerService.getAllLedgers(pageable));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryLedgerDTO>> getLedgersByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ledgerService.getLedgersByProduct(productId));
    }
}
