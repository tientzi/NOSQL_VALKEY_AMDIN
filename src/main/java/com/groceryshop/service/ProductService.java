package com.groceryshop.service;

import com.groceryshop.dto.ProductDTO;
import com.groceryshop.entity.*;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private InventoryLedgerService ledgerService;

    public List<ProductDTO> getAllProductsAdmin() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(EntityMapper::toProductDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> getFilteredProducts(String keyword, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("isActive")));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.toLowerCase().trim() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), likeKeyword);
                Predicate descLike = cb.like(cb.lower(root.get("description")), likeKeyword);
                predicates.add(cb.or(nameLike, descLike));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            if (brandId != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), brandId));
            }

            if (minPrice != null) {
                predicates.add(cb.ge(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.le(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortBy != null) {
            switch (sortBy) {
                case "price_asc":
                    sort = Sort.by(Sort.Direction.ASC, "price");
                    break;
                case "price_desc":
                    sort = Sort.by(Sort.Direction.DESC, "price");
                    break;
                case "newest":
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
                case "best_seller":
                    sort = Sort.by(Sort.Direction.DESC, "id");
                    break;
            }
        }

        return productRepository.findAll(spec, sort).stream()
                .map(EntityMapper::toProductDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        return EntityMapper.toProductDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        if (productRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Tên sản phẩm đã tồn tại");
        }

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + dto.getCategoryId()));

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + dto.getBrandId()));

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .salePrice(dto.getSalePrice())
                .category(category)
                .brand(brand)
                .mainImage(dto.getMainImage())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .unit(dto.getUnit() != null ? dto.getUnit() : "Cái")
                .weightG(dto.getWeightG() != null ? dto.getWeightG() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        Product saved = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(saved)
                .currentStock(0) // Khóa nhập tay: Tồn kho mặc định bằng 0, chỉ tăng qua Nhập kho & Kiểm định QC
                .minimumStock(dto.getMinimumStock() != null ? dto.getMinimumStock() : 5)
                .location(dto.getLocation() != null ? dto.getLocation() : "Khu vực mặc định")
                .build();
        inventoryRepository.save(inventory);
        saved.setInventory(inventory);

        if (dto.getImages() != null) {
            for (String imgPath : dto.getImages()) {
                ProductImage productImage = ProductImage.builder()
                        .product(saved)
                        .imagePath(imgPath)
                        .build();
                productImageRepository.save(productImage);
                saved.getImages().add(productImage);
            }
        }

        return EntityMapper.toProductDTO(saved);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));

        productRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Tên sản phẩm đã được sử dụng");
                    }
                });

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + dto.getCategoryId()));

        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + dto.getBrandId()));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setSalePrice(dto.getSalePrice());
        product.setCategory(category);
        product.setBrand(brand);
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getBarcode() != null) product.setBarcode(dto.getBarcode());
        if (dto.getUnit() != null) product.setUnit(dto.getUnit());
        if (dto.getWeightG() != null) product.setWeightG(dto.getWeightG());
        
        if (dto.getMainImage() != null) {
            product.setMainImage(dto.getMainImage());
        }
        if (dto.getIsActive() != null) {
            product.setIsActive(dto.getIsActive());
        }

        Inventory inventory = product.getInventory();
        if (inventory == null) {
            inventory = Inventory.builder().product(product).build();
        }
        // Khóa chỉnh sửa tồn kho trực tiếp qua form sản phẩm (Tồn kho chỉ thay đổi qua Nhập kho/Bán hàng/Kiểm định)
        if (dto.getMinimumStock() != null) {
            inventory.setMinimumStock(dto.getMinimumStock());
        }
        if (dto.getLocation() != null) {
            inventory.setLocation(dto.getLocation());
        }
        inventoryRepository.save(inventory);
        product.setInventory(inventory);

        if (dto.getImages() != null) {
            productImageRepository.deleteAll(product.getImages());
            product.getImages().clear();
            for (String imgPath : dto.getImages()) {
                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imagePath(imgPath)
                        .build();
                productImageRepository.save(productImage);
                product.getImages().add(productImage);
            }
        }

        Product updated = productRepository.save(product);
        return EntityMapper.toProductDTO(updated);
    }

    @Transactional
    public ProductDTO toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        boolean current = product.getIsActive() != null ? product.getIsActive() : true;
        product.setIsActive(!current);
        Product updated = productRepository.save(product);
        return EntityMapper.toProductDTO(updated);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        productRepository.delete(product);
    }
}
