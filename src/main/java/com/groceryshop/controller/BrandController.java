package com.groceryshop.controller;

import com.groceryshop.dto.BrandDTO;
import com.groceryshop.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class BrandController {

    @Autowired
    private BrandService brandService;

    @GetMapping("/api/public/brands")
    public ResponseEntity<List<BrandDTO>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @GetMapping("/api/admin/brands")
    public ResponseEntity<List<BrandDTO>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @GetMapping("/api/admin/brands/{id}")
    public ResponseEntity<BrandDTO> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @PostMapping("/api/admin/brands")
    public ResponseEntity<BrandDTO> createBrand(@RequestBody BrandDTO dto) {
        return ResponseEntity.ok(brandService.createBrand(dto));
    }

    @PutMapping("/api/admin/brands/{id}")
    public ResponseEntity<BrandDTO> updateBrand(@PathVariable Long id, @RequestBody BrandDTO dto) {
        return ResponseEntity.ok(brandService.updateBrand(id, dto));
    }

    @DeleteMapping("/api/admin/brands/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok().body("{\"message\": \"Xóa thương hiệu thành công\"}");
    }
}
