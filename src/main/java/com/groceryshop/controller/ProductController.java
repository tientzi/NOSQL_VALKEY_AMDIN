package com.groceryshop.controller;

import com.groceryshop.dto.ProductDTO;
import com.groceryshop.dto.ProductRecommendationDTO;
import com.groceryshop.service.FileStorageService;
import com.groceryshop.service.ProductService;
import com.groceryshop.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/api/public/products")
    public ResponseEntity<List<ProductDTO>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sortBy) {
        
        List<ProductDTO> products = productService.getFilteredProducts(keyword, categoryId, brandId, minPrice, maxPrice, sortBy);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/api/public/products/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/api/public/products/{id}/recommendations")
    public ResponseEntity<List<ProductRecommendationDTO>> getRecommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(recommendationService.getRecommendations(id, limit));
    }

    @GetMapping("/api/admin/products")
    public ResponseEntity<List<ProductDTO>> getAllProductsAdmin() {
        return ResponseEntity.ok(productService.getAllProductsAdmin());
    }

    @PostMapping("/api/admin/products")
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.createProduct(dto));
    }

    @PutMapping("/api/admin/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @PatchMapping("/api/admin/products/{id}/toggle-status")
    public ResponseEntity<ProductDTO> toggleProductStatus(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleProductStatus(id));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().body("{\"message\": \"Xóa sản phẩm thành công\"}");
    }

    @PostMapping("/api/admin/products/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.saveFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);
        } catch (IOException | IllegalArgumentException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/api/admin/products/upload-multiple")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String fileUrl = fileStorageService.saveFile(file);
                urls.add(fileUrl);
            } catch (IOException | IllegalArgumentException e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("urls", urls);
        if (!errors.isEmpty()) {
            response.put("errors", errors);
        }
        return ResponseEntity.ok(response);
    }
}
