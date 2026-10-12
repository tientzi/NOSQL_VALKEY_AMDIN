package com.groceryshop.service;

import com.groceryshop.dto.ProductBatchDTO;
import com.groceryshop.entity.Inventory;
import com.groceryshop.entity.Product;
import com.groceryshop.entity.ProductBatch;
import com.groceryshop.entity.User;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.InventoryRepository;
import com.groceryshop.repository.ProductBatchRepository;
import com.groceryshop.repository.ProductRepository;
import com.groceryshop.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductBatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryLedgerService ledgerService;
    private final UserRepository userRepository;

    @Autowired
    public ProductBatchService(ProductBatchRepository batchRepository,
                               ProductRepository productRepository,
                               InventoryRepository inventoryRepository,
                               InventoryLedgerService ledgerService,
                               UserRepository userRepository) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.ledgerService = ledgerService;
        this.userRepository = userRepository;
    }

    @PostConstruct
    @Transactional
    public void syncLegacyBatchImportPrices() {
        try {
            List<ProductBatch> batches = batchRepository.findAll();
            for (ProductBatch b : batches) {
                boolean needSave = false;
                if (b.getImportPrice() == null) {
                    b.setImportPrice(b.getEffectiveImportPrice());
                    needSave = true;
                }
                if (b.getStatus() == null) {
                    boolean isExp = b.getExpiryDate() != null && b.getExpiryDate().isBefore(LocalDate.now());
                    b.setStatus(isExp ? (b.getQuantity() != null && b.getQuantity() > 0 ? "EXPIRED" : "DISPOSED") : "ACTIVE");
                    needSave = true;
                }
                if (needSave) {
                    batchRepository.save(b);
                }
            }
        } catch (Exception e) {
            System.err.println("[ProductBatchService] Sync legacy batches notice: " + e.getMessage());
        }
    }

    public List<ProductBatchDTO> getAllBatches() {
        return batchRepository.findAll().stream()
                .filter(b -> b.getQuantity() != null && b.getQuantity() > 0)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductBatchDTO> getExpiringBatches(int days) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(days);
        return batchRepository.findAll().stream()
                .filter(b -> b.getQuantity() != null && b.getQuantity() > 0 
                        && b.getExpiryDate() != null 
                        && !b.getExpiryDate().isBefore(today) // Chưa hết hạn hôm nay
                        && !b.getExpiryDate().isAfter(threshold)) // Cận date trong khoảng days
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProductBatchDTO> getExpiredBatches() {
        LocalDate today = LocalDate.now();
        return batchRepository.findAll().stream()
                .filter(b -> b.getExpiryDate() != null && b.getExpiryDate().isBefore(today))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void disposeBatch(Long batchId, Long userId) {
        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng với id: " + batchId));

        if (batch.getQuantity() == null || batch.getQuantity() <= 0) {
            throw new BadRequestException("Lô hàng này đã có số lượng tồn bằng 0 hoặc đã được xuất hủy trước đó");
        }

        int qtyToDispose = batch.getQuantity();
        BigDecimal importPrice = batch.getEffectiveImportPrice();
        BigDecimal totalLoss = importPrice.multiply(BigDecimal.valueOf(qtyToDispose));

        batch.setQuantity(0);
        batch.setStatus("DISPOSED");
        batchRepository.save(batch);

        Product product = batch.getProduct();
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        if (product != null && product.getInventory() != null) {
            Inventory inv = product.getInventory();
            int newStock = Math.max(0, inv.getCurrentStock() - qtyToDispose);
            inv.setCurrentStock(newStock);
            inventoryRepository.save(inv);

            ledgerService.recordLog(
                    inv,
                    "EXPIRED_DISPOSAL",
                    -qtyToDispose,
                    batch.getId(),
                    "Xuất hủy lô quá hạn: " + batch.getBatchName() + " (Thiệt hại vốn: " + totalLoss + "đ)",
                    user
            );
        }
    }

    @Transactional
    public void applyBatchClearanceSale(Long batchId, Integer discountPercentage, Double customSalePrice) {
        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng với id: " + batchId));

        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Lô hàng đã hết hạn sử dụng! Bắt buộc không được phép thiết lập Sale bán hàng.");
        }

        Product product = batch.getProduct();
        if (product == null) {
            throw new BadRequestException("Lô hàng không liên kết với sản phẩm hợp lệ");
        }

        BigDecimal currentPrice = product.getPrice();
        BigDecimal salePrice;

        if (customSalePrice != null && customSalePrice > 0) {
            salePrice = BigDecimal.valueOf(customSalePrice);
        } else if (discountPercentage != null && discountPercentage > 0 && discountPercentage < 100) {
            salePrice = currentPrice.multiply(BigDecimal.valueOf(100 - discountPercentage))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            throw new BadRequestException("Vui lòng nhập % chiết khấu hợp lệ (1 - 99%) hoặc giá sale mong muốn");
        }

        if (salePrice.compareTo(currentPrice) >= 0) {
            throw new BadRequestException("Giá xả kho phải nhỏ hơn giá gốc hiện tại (" + currentPrice + "đ)");
        }

        batch.setSalePrice(salePrice);
        batchRepository.save(batch);

        // Đồng thời cập nhật giá sale của sản phẩm để hiển thị ưu đãi trên storefront
        product.setSalePrice(salePrice);
        productRepository.save(product);
    }

    @Transactional
    public void applyClearanceSale(Long productId, Integer discountPercentage, Double customSalePrice) {
        if (productId == null) {
            throw new BadRequestException("ID sản phẩm không được để trống");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại với id: " + productId));

        // Kiểm tra xem sản phẩm có lô hàng nào còn hạn không
        LocalDate today = LocalDate.now();
        List<ProductBatch> validBatches = batchRepository.findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, 0)
                .stream()
                .filter(b -> b.getExpiryDate() != null && !b.getExpiryDate().isBefore(today))
                .collect(Collectors.toList());

        if (validBatches.isEmpty()) {
            throw new BadRequestException("Sản phẩm này hiện không có lô hàng nào còn hạn sử dụng để thiết lập Sale!");
        }

        BigDecimal currentPrice = product.getPrice();
        BigDecimal salePrice;

        if (customSalePrice != null && customSalePrice > 0) {
            salePrice = BigDecimal.valueOf(customSalePrice);
        } else if (discountPercentage != null && discountPercentage > 0 && discountPercentage < 100) {
            salePrice = currentPrice.multiply(BigDecimal.valueOf(100 - discountPercentage))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            throw new BadRequestException("Vui lòng nhập % chiết khấu hợp lệ (1 - 99%) hoặc giá sale mong muốn");
        }

        if (salePrice.compareTo(currentPrice) >= 0) {
            throw new BadRequestException("Giá xả kho phải nhỏ hơn giá gốc hiện tại (" + currentPrice + "đ)");
        }

        // Áp dụng cho lô cận date nhất còn hạn
        ProductBatch nearestValidBatch = validBatches.get(0);
        nearestValidBatch.setSalePrice(salePrice);
        batchRepository.save(nearestValidBatch);

        product.setSalePrice(salePrice);
        productRepository.save(product);
    }

    @Transactional
    public void applyClearanceSale(Long productId, Integer discountPercentage) {
        applyClearanceSale(productId, discountPercentage, null);
    }

    private ProductBatchDTO toDTO(ProductBatch batch) {
        return EntityMapper.toProductBatchDTO(batch);
    }
}
