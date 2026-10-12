package com.groceryshop.controller;

import com.groceryshop.dto.GoodsReceiptDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.GoodsReceiptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/goods-receipts")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class GoodsReceiptController {

    @Autowired
    private GoodsReceiptService receiptService;

    @GetMapping
    public ResponseEntity<Page<GoodsReceiptDTO>> getAllReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(receiptService.getAllReceipts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptDTO> getReceiptById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getReceiptById(id));
    }

    @PostMapping
    public ResponseEntity<GoodsReceiptDTO> createReceipt(@AuthenticationPrincipal UserPrincipal userDetails, @Valid @RequestBody GoodsReceiptDTO receiptDTO) {
        return ResponseEntity.ok(receiptService.createReceipt(userDetails.getId(), receiptDTO));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<GoodsReceiptDTO> completeReceipt(@AuthenticationPrincipal UserPrincipal userDetails, @PathVariable Long id) {
        return ResponseEntity.ok(receiptService.completeReceipt(userDetails.getId(), id));
    }

    @PostMapping("/{id}/qc-inspection")
    public ResponseEntity<GoodsReceiptDTO> inspectAndCompleteReceipt(
            @AuthenticationPrincipal UserPrincipal userDetails,
            @PathVariable Long id,
            @RequestBody com.groceryshop.dto.QCInspectionRequestDTO request) {
        return ResponseEntity.ok(receiptService.inspectAndCompleteReceipt(userDetails.getId(), id, request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReceipt(@PathVariable Long id) {
        receiptService.cancelReceipt(id);
        return ResponseEntity.ok().build();
    }
}
