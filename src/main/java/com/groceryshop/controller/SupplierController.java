package com.groceryshop.controller;

import com.groceryshop.dto.SupplierDTO;
import com.groceryshop.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/suppliers")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        return ResponseEntity.ok(supplierService.createSupplier(supplierDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO supplierDTO) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDTO));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<SupplierDTO> toggleSupplierStatus(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.toggleSupplierStatus(id));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<com.groceryshop.dto.SupplierProductDTO>> getProductsBySupplier(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getProductsBySupplier(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok().build();
    }
}
