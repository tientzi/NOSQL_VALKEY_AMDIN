package com.groceryshop.service;

import com.groceryshop.dto.GoodsReceiptDTO;
import com.groceryshop.dto.GoodsReceiptItemDTO;
import com.groceryshop.dto.QCInspectionRequestDTO;
import com.groceryshop.entity.*;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.ProductBatchRepository;
import com.groceryshop.repository.GoodsReceiptItemRepository;
import com.groceryshop.repository.GoodsReceiptRepository;
import com.groceryshop.repository.InventoryRepository;
import com.groceryshop.repository.ProductRepository;
import com.groceryshop.repository.SupplierRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptService {

    @Autowired
    private GoodsReceiptRepository receiptRepository;

    @Autowired
    private GoodsReceiptItemRepository itemRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.groceryshop.repository.BrandRepository brandRepository;

    @Autowired
    private InventoryLedgerService ledgerService;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    public Page<GoodsReceiptDTO> getAllReceipts(Pageable pageable) {
        return receiptRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    public GoodsReceiptDTO getReceiptById(Long id) {
        GoodsReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));
        return toDTO(receipt);
    }

    @Transactional
    public GoodsReceiptDTO createReceipt(Long userId, GoodsReceiptDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));

        Brand brand = null;
        if (dto.getBrandId() != null) {
            brand = brandRepository.findById(dto.getBrandId()).orElse(null);
        }

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BadRequestException("Phiếu nhập phải có ít nhất 1 sản phẩm");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        GoodsReceipt receipt = GoodsReceipt.builder()
                .supplier(supplier)
                .brand(brand)
                .totalAmount(totalAmount)
                .note(dto.getNote())
                .status("DRAFT")
                .createdBy(user)
                .build();
                
        GoodsReceipt savedReceipt = receiptRepository.save(receipt);

        for (GoodsReceiptItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm id: " + itemDTO.getProductId()));
            
            GoodsReceiptItem item = GoodsReceiptItem.builder()
                    .goodsReceipt(savedReceipt)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .importPrice(itemDTO.getImportPrice())
                    .batchName(itemDTO.getBatchName())
                    .expiryDate(itemDTO.getExpiryDate())
                    .build();
                    
            itemRepository.save(item);
            savedReceipt.getItems().add(item);

            totalAmount = totalAmount.add(itemDTO.getImportPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        savedReceipt.setTotalAmount(totalAmount);
        return toDTO(receiptRepository.save(savedReceipt));
    }

    @Transactional
    public GoodsReceiptDTO completeReceipt(Long userId, Long receiptId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

        if (!receipt.getStatus().equals("DRAFT")) {
            throw new BadRequestException("Chỉ có thể hoàn thành phiếu nhập ở trạng thái DRAFT");
        }

        receipt.setStatus("COMPLETED");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        for (GoodsReceiptItem item : receipt.getItems()) {
            Product product = item.getProduct();
            Inventory inventory = product.getInventory();
            if (inventory == null) {
                inventory = Inventory.builder().product(product).currentStock(0).minimumStock(5).build();
            }
            
            // Mặc định hoàn thành nhanh: 100% đạt chuẩn
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setPassedQuantity(qty);
            item.setRejectedQuantity(0);
            item.setQcStatus("PASSED");
            item.setInspectedAt(now);
            item.setInspectedBy(user);
            itemRepository.save(item);

            inventory.setCurrentStock(inventory.getCurrentStock() + qty);
            inventoryRepository.save(inventory);

            ledgerService.recordLog(
                    inventory,
                    "IMPORT",
                    qty,
                    receipt.getId(),
                    "Nhập hàng từ phiếu #" + receipt.getId() + " (Đạt chuẩn 100%)",
                    user
            );

            if (item.getBatchName() != null && item.getExpiryDate() != null) {
                ProductBatch batch = ProductBatch.builder()
                        .product(product)
                        .goodsReceipt(receipt)
                        .batchName(item.getBatchName())
                        .quantity(qty)
                        .expiryDate(item.getExpiryDate())
                        .importPrice(item.getImportPrice())
                        .status("ACTIVE")
                        .build();
                productBatchRepository.save(batch);
            }
        }

        return toDTO(receiptRepository.save(receipt));
    }

    @Transactional
    public GoodsReceiptDTO inspectAndCompleteReceipt(Long userId, Long receiptId, QCInspectionRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));

        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));

        if (!"DRAFT".equals(receipt.getStatus()) && !"PENDING_QC".equals(receipt.getStatus())) {
            throw new BadRequestException("Chỉ có thể kiểm định phiếu nhập ở trạng thái DRAFT hoặc PENDING_QC");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Dữ liệu kiểm định không được để trống");
        }

        java.util.Map<Long, QCInspectionRequestDTO.ItemQCResult> resultMap = request.getItems().stream()
                .filter(it -> it.getItemId() != null)
                .collect(Collectors.toMap(QCInspectionRequestDTO.ItemQCResult::getItemId, it -> it, (a, b) -> a));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        BigDecimal actualReceivedTotal = BigDecimal.ZERO;

        for (GoodsReceiptItem item : receipt.getItems()) {
            QCInspectionRequestDTO.ItemQCResult qc = resultMap.get(item.getId());
            int totalQty = item.getQuantity() != null ? item.getQuantity() : 0;
            int passedQty = totalQty;
            int rejectedQty = 0;
            String reason = null;
            String note = null;

            if (qc != null) {
                passedQty = qc.getPassedQuantity() != null ? qc.getPassedQuantity() : 0;
                rejectedQty = qc.getRejectedQuantity() != null ? qc.getRejectedQuantity() : (totalQty - passedQty);
                if (passedQty < 0 || rejectedQty < 0 || (passedQty + rejectedQty != totalQty)) {
                    throw new BadRequestException("Mặt hàng [" + item.getProduct().getName() + "]: Số lượng đạt (" + passedQty + ") + không đạt (" + rejectedQty + ") phải bằng tổng số lượng nhập (" + totalQty + ")");
                }
                reason = qc.getRejectReason();
                note = qc.getQcNote();
            }

            // Lưu thông tin QC vào chi tiết phiếu
            item.setPassedQuantity(passedQty);
            item.setRejectedQuantity(rejectedQty);
            item.setRejectReason(reason);
            item.setQcNote(note);
            item.setInspectedAt(now);
            item.setInspectedBy(user);

            if (passedQty == totalQty) {
                item.setQcStatus("PASSED");
            } else if (passedQty == 0) {
                item.setQcStatus("REJECTED");
            } else {
                item.setQcStatus("PARTIALLY_PASSED");
            }
            itemRepository.save(item);

            // LOGIC CỐT LÕI: CHỈ TĂNG TỒN KHO CHO SỐ LƯỢNG ĐẠT CHUẨN
            Product product = item.getProduct();
            Inventory inventory = product.getInventory();
            if (inventory == null) {
                inventory = Inventory.builder().product(product).currentStock(0).minimumStock(5).build();
            }

            if (passedQty > 0) {
                inventory.setCurrentStock(inventory.getCurrentStock() + passedQty);
                inventoryRepository.save(inventory);

                String ledgerNote = "Nhập kho sau kiểm định QC: " + passedQty + " đạt chuẩn";
                if (rejectedQty > 0) {
                    ledgerNote += ", " + rejectedQty + " trả về NCC (Lý do: " + (reason != null ? reason : "Lỗi chất lượng") + ")";
                }
                ledgerNote += " [PNK #" + receipt.getId() + "]";

                ledgerService.recordLog(
                        inventory,
                        "IMPORT",
                        passedQty,
                        receipt.getId(),
                        ledgerNote,
                        user
                );

                // LÔ HÀNG (PRODUCT BATCH): Chỉ tạo với số lượng đạt chuẩn (passedQty)
                if (item.getBatchName() != null && item.getExpiryDate() != null) {
                    ProductBatch batch = ProductBatch.builder()
                            .product(product)
                            .goodsReceipt(receipt)
                            .batchName(item.getBatchName())
                            .quantity(passedQty)
                            .expiryDate(item.getExpiryDate())
                            .importPrice(item.getImportPrice())
                            .status("ACTIVE")
                            .build();
                    productBatchRepository.save(batch);
                }
            } else {
                // Toàn bộ lô bị trả về NCC
                ledgerService.recordLog(
                        inventory,
                        "RETURN",
                        0,
                        receipt.getId(),
                        "Trả về NCC " + rejectedQty + " sp không đạt kiểm định QC (Lý do: " + (reason != null ? reason : "Lỗi chất lượng") + ") [PNK #" + receipt.getId() + "]",
                        user
                );
            }

            actualReceivedTotal = actualReceivedTotal.add(item.getImportPrice().multiply(BigDecimal.valueOf(passedQty)));
        }

        if (request.getGeneralNote() != null && !request.getGeneralNote().isBlank()) {
            receipt.setNote((receipt.getNote() != null ? receipt.getNote() + " | " : "") + "QC: " + request.getGeneralNote());
        }

        receipt.setTotalAmount(actualReceivedTotal);
        receipt.setStatus("COMPLETED");
        return toDTO(receiptRepository.save(receipt));
    }

    @Transactional
    public void cancelReceipt(Long receiptId) {
        GoodsReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập"));
                
        if (!receipt.getStatus().equals("DRAFT")) {
            throw new BadRequestException("Chỉ có thể hủy phiếu nhập ở trạng thái DRAFT");
        }
        
        receipt.setStatus("CANCELLED");
        receiptRepository.save(receipt);
    }

    private GoodsReceiptDTO toDTO(GoodsReceipt receipt) {
        return com.groceryshop.mapper.EntityMapper.toGoodsReceiptDTO(receipt);
    }
}
