package com.groceryshop.service;

import com.groceryshop.dto.SupplierDTO;
import com.groceryshop.entity.Supplier;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private com.groceryshop.repository.GoodsReceiptItemRepository goodsReceiptItemRepository;

    @Autowired
    private com.groceryshop.repository.GoodsReceiptRepository goodsReceiptRepository;

    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
        return toDTO(supplier);
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO dto) {
        if (supplierRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Tên nhà cung cấp đã tồn tại");
        }
        Supplier supplier = Supplier.builder()
                .name(dto.getName())
                .contactName(dto.getContactName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
        return toDTO(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
                
        supplierRepository.findByName(dto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) throw new BadRequestException("Tên nhà cung cấp đã tồn tại");
        });

        supplier.setName(dto.getName());
        supplier.setContactName(dto.getContactName());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        if (dto.getIsActive() != null) {
            supplier.setIsActive(dto.getIsActive());
        }

        return toDTO(supplierRepository.save(supplier));
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
        if (goodsReceiptRepository.existsBySupplierId(id)) {
            throw new BadRequestException("Nhà cung cấp đã có phiếu nhập kho liên kết, không thể xóa để đảm bảo lịch sử dữ liệu. Vui lòng chuyển trạng thái sang Vô hiệu hóa (Ngừng hợp tác)!");
        }
        supplierRepository.delete(supplier);
    }

    @Transactional
    public SupplierDTO toggleSupplierStatus(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));
        boolean current = supplier.getIsActive() != null ? supplier.getIsActive() : true;
        supplier.setIsActive(!current);
        return toDTO(supplierRepository.save(supplier));
    }

    public List<com.groceryshop.dto.SupplierProductDTO> getProductsBySupplier(Long supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà cung cấp"));

        List<com.groceryshop.entity.GoodsReceiptItem> items = goodsReceiptItemRepository.findByGoodsReceiptSupplierId(supplierId);

        java.util.Map<Long, List<com.groceryshop.entity.GoodsReceiptItem>> grouped = items.stream()
                .filter(i -> i.getProduct() != null)
                .collect(Collectors.groupingBy(i -> i.getProduct().getId()));

        List<com.groceryshop.dto.SupplierProductDTO> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, List<com.groceryshop.entity.GoodsReceiptItem>> entry : grouped.entrySet()) {
            List<com.groceryshop.entity.GoodsReceiptItem> prodItems = entry.getValue();
            com.groceryshop.entity.Product p = prodItems.get(0).getProduct();

            int totalQty = prodItems.stream()
                    .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                    .sum();

            java.time.LocalDateTime lastImport = prodItems.stream()
                    .map(i -> i.getGoodsReceipt() != null ? i.getGoodsReceipt().getCreatedAt() : null)
                    .filter(java.util.Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo)
                    .orElse(null);

            com.groceryshop.dto.SupplierProductDTO dto = com.groceryshop.dto.SupplierProductDTO.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .sku(p.getSku())
                    .mainImage(p.getMainImage())
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                    .price(p.getPrice())
                    .isActive(p.getIsActive() != null ? p.getIsActive() : true)
                    .currentStock(p.getInventory() != null ? p.getInventory().getCurrentStock() : 0)
                    .totalSuppliedQuantity(totalQty)
                    .lastImportDate(lastImport)
                    .build();

            result.add(dto);
        }

        result.sort((a, b) -> {
            if (a.getLastImportDate() == null) return 1;
            if (b.getLastImportDate() == null) return -1;
            return b.getLastImportDate().compareTo(a.getLastImportDate());
        });

        return result;
    }

    private SupplierDTO toDTO(Supplier supplier) {
        return com.groceryshop.mapper.EntityMapper.toSupplierDTO(supplier);
    }
}
