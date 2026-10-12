package com.groceryshop.mapper;

import com.groceryshop.dto.*;
import com.groceryshop.entity.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class EntityMapper {

    public static CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .image(category.getImage())
                .isActive(category.getIsActive())
                .build();
    }

    public static Category toCategoryEntity(CategoryDTO dto) {
        if (dto == null) return null;
        return Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .image(dto.getImage())
                .isActive(dto.getIsActive())
                .build();
    }

    public static BrandDTO toBrandDTO(Brand brand) {
        if (brand == null) return null;
        return BrandDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .isActive(brand.getIsActive())
                .build();
    }

    public static Brand toBrandEntity(BrandDTO dto) {
        if (dto == null) return null;
        return Brand.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(dto.getIsActive())
                .build();
    }

    public static ProductDTO toProductDTO(Product product) {
        if (product == null) return null;
        ProductDTO.ProductDTOBuilder builder = ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .mainImage(product.getMainImage())
                .isActive(product.getIsActive())
                .sku(product.getSku())
                .barcode(product.getBarcode())
                .unit(product.getUnit())
                .weightG(product.getWeightG())
                .images(product.getImages() != null ? 
                        product.getImages().stream().map(ProductImage::getImagePath).collect(Collectors.toList()) : null);

        if (product.getInventory() != null) {
            builder.currentStock(product.getInventory().getCurrentStock())
                   .minimumStock(product.getInventory().getMinimumStock())
                   .location(product.getInventory().getLocation());
        } else {
            builder.currentStock(0).minimumStock(5);
        }

        return builder.build();
    }

    public static CartItemDTO toCartItemDTO(CartItem item) {
        if (item == null) return null;
        Product product = item.getProduct();
        return CartItemDTO.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : "Sản phẩm không khả dụng")
                .productMainImage(product != null ? product.getMainImage() : null)
                .productPrice(product != null ? product.getPrice() : BigDecimal.ZERO)
                .productSalePrice(product != null ? product.getSalePrice() : null)
                .quantity(item.getQuantity())
                .maxStock(product != null && product.getInventory() != null ? product.getInventory().getCurrentStock() : 0)
                .categoryId(product != null && product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product != null && product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }

    public static CartDTO toCartDTO(Cart cart) {
        if (cart == null) return null;
        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .items(cart.getItems() != null ? 
                        cart.getItems().stream().map(EntityMapper::toCartItemDTO).collect(Collectors.toList()) : null)
                .build();
    }

    public static OrderItemDTO toOrderItemDTO(OrderItem item) {
        if (item == null) return null;
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    public static OrderDTO toOrderDTO(Order order) {
        if (order == null) return null;

        String payStatus = "PENDING";
        if (order.getPayment() != null && order.getPayment().getPaymentStatus() != null) {
            payStatus = order.getPayment().getPaymentStatus();
        } else if ("HOAN_THANH".equalsIgnoreCase(order.getStatus()) || "DA_GIAO".equalsIgnoreCase(order.getStatus())) {
            payStatus = "COMPLETED";
        }

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .username(order.getUser() != null ? order.getUser().getUsername() : null)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(payStatus)
                .couponCode(order.getCouponCode())
                .note(order.getNote())
                .shipperId(order.getShipper() != null ? order.getShipper().getId() : null)
                .shipperName(order.getShipper() != null ? (order.getShipper().getFullName() != null ? order.getShipper().getFullName() : order.getShipper().getUsername()) : null)
                .shipperPhone(order.getShipper() != null ? order.getShipper().getPhone() : null)
                .deliveryNote(order.getDeliveryNote())
                .deliveredAt(order.getDeliveredAt())
                .deliveryFailedReason(order.getDeliveryFailedReason())
                .detectedDistrict(detectDistrict(order.getShippingAddress()))
                .membershipTier(order.getUser() != null ? order.getUser().getMembershipTier() : null)
                .tierDiscountAmount(calculateTierDiscount(order))
                .idempotencyKey(order.getIdempotencyKey())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems() != null ? 
                        order.getItems().stream().map(EntityMapper::toOrderItemDTO).collect(Collectors.toList()) : null)
                .build();
    }

    private static BigDecimal calculateTierDiscount(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getMembershipTier() == null || order.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        if (order.getDiscountAmount() == null || order.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        String tier = order.getUser().getMembershipTier().trim().toUpperCase();
        BigDecimal rate = BigDecimal.ZERO;
        if ("SILVER".equals(tier)) rate = BigDecimal.valueOf(0.02);
        else if ("GOLD".equals(tier)) rate = BigDecimal.valueOf(0.05);
        else if ("DIAMOND".equals(tier)) rate = BigDecimal.valueOf(0.08);

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal calculated = order.getTotalAmount().multiply(rate);
        return calculated.min(order.getDiscountAmount());
    }

    public static String detectDistrict(String address) {
        if (address == null) return "Khu vực khác";
        String lower = address.toLowerCase();
        if (lower.contains("tân phú") || lower.contains("tan phu") || lower.contains("q. tân phú") || lower.contains("q tan phu")) {
            return "Quận Tân Phú";
        } else if (lower.contains("tân bình") || lower.contains("tan binh") || lower.contains("q. tân bình") || lower.contains("q tan binh")) {
            return "Quận Tân Bình";
        } else if (lower.contains("quận 12") || lower.contains("quan 12") || lower.contains("q.12") || lower.contains("q12") || lower.contains("q. 12") || lower.contains("q 12")) {
            return "Quận 12";
        } else if (lower.contains("quận 1") || lower.contains("quan 1") || lower.contains("q.1") || lower.contains("q1") || lower.contains("q. 1") || lower.contains("q 1")) {
            return "Quận 1";
        } else if (lower.contains("cầu giấy") || lower.contains("cau giay")) {
            return "Quận Cầu Giấy";
        } else if (lower.contains("ba đình") || lower.contains("ba dinh")) {
            return "Quận Ba Đình";
        }
        return "Khu vực khác";
    }

    public static CouponDTO toCouponDTO(Coupon coupon) {
        if (coupon == null) return null;
        return CouponDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .minOrderAmount(coupon.getMinOrderAmount())
                .usedCount(coupon.getUsedCount())
                .maxUses(coupon.getMaxUses())
                .isActive(coupon.getIsActive())
                .applicableCategoryId(coupon.getApplicableCategory() != null ? coupon.getApplicableCategory().getId() : null)
                .applicableCategoryName(coupon.getApplicableCategory() != null ? coupon.getApplicableCategory().getName() : null)
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .build();
    }

    public static Coupon toCouponEntity(CouponDTO dto) {
        if (dto == null) return null;
        return Coupon.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .description(dto.getDescription())
                .discountType(dto.getDiscountType())
                .discountValue(dto.getDiscountValue())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .minOrderAmount(dto.getMinOrderAmount())
                .usedCount(dto.getUsedCount())
                .maxUses(dto.getMaxUses())
                .isActive(dto.getIsActive())
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .build();
    }

    public static ReviewDTO toReviewDTO(Review review) {
        if (review == null) return null;
        return ReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .username(review.getUser() != null ? review.getUser().getUsername() : null)
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .isApproved(review.getIsApproved())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public static UserDTO toUserDTO(User user) {
        if (user == null) return null;
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .isActive(user.getIsActive())
                .loyaltyPoints(user.getLoyaltyPoints())
                .membershipTier(user.getMembershipTier())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static AddressDTO toAddressDTO(Address address) {
        if (address == null) return null;
        return AddressDTO.builder()
                .id(address.getId())
                .userId(address.getUser() != null ? address.getUser().getId() : null)
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.getIsDefault())
                .build();
    }

    public static Address toAddressEntity(AddressDTO dto) {
        if (dto == null) return null;
        return Address.builder()
                .id(dto.getId())
                .receiverName(dto.getReceiverName())
                .receiverPhone(dto.getReceiverPhone())
                .province(dto.getProvince())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .detailAddress(dto.getDetailAddress())
                .isDefault(dto.getIsDefault())
                .build();
    }

    public static ProductBatchDTO toProductBatchDTO(ProductBatch batch) {
        if (batch == null) return null;
        Product p = batch.getProduct();
        BigDecimal originalPrice = p != null ? p.getPrice() : null;
        BigDecimal salePrice = batch.getSalePrice() != null ? batch.getSalePrice() : (p != null ? p.getSalePrice() : null);
        
        Integer discount = null;
        if (originalPrice != null && salePrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (salePrice.compareTo(originalPrice) < 0) {
                BigDecimal diff = originalPrice.subtract(salePrice);
                discount = diff.multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 0, RoundingMode.HALF_UP)
                        .intValue();
            }
        }

        BigDecimal importPrice = batch.getEffectiveImportPrice();
        boolean isExpired = batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now());
        Long daysRemaining = batch.getExpiryDate() != null ? java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate()) : null;
        
        BigDecimal totalLoss = BigDecimal.ZERO;
        if (isExpired && batch.getQuantity() != null && batch.getQuantity() > 0 && importPrice != null) {
            totalLoss = importPrice.multiply(BigDecimal.valueOf(batch.getQuantity()));
        }

        return ProductBatchDTO.builder()
                .id(batch.getId())
                .productId(p != null ? p.getId() : null)
                .productName(p != null ? p.getName() : null)
                .productSku(p != null ? p.getSku() : null)
                .goodsReceiptId(batch.getGoodsReceipt() != null ? batch.getGoodsReceipt().getId() : null)
                .batchName(batch.getBatchName())
                .quantity(batch.getQuantity())
                .expiryDate(batch.getExpiryDate())
                .discountPercentage(discount)
                .importPrice(importPrice)
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .status(batch.getStatus() != null ? batch.getStatus() : (isExpired ? "EXPIRED" : "ACTIVE"))
                .isExpired(isExpired)
                .daysRemaining(daysRemaining)
                .totalLoss(totalLoss)
                .createdAt(batch.getCreatedAt())
                .build();
    }

    public static SupplierDTO toSupplierDTO(Supplier supplier) {
        if (supplier == null) return null;
        return SupplierDTO.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .build();
    }

    public static GoodsReceiptItemDTO toGoodsReceiptItemDTO(GoodsReceiptItem item) {
        if (item == null) return null;
        return GoodsReceiptItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : null)
                .sku(item.getProduct() != null ? item.getProduct().getSku() : null)
                .brandId(item.getProduct() != null && item.getProduct().getBrand() != null ? item.getProduct().getBrand().getId() : null)
                .brandName(item.getProduct() != null && item.getProduct().getBrand() != null ? item.getProduct().getBrand().getName() : null)
                .quantity(item.getQuantity())
                .importPrice(item.getImportPrice())
                .batchName(item.getBatchName())
                .expiryDate(item.getExpiryDate())
                .passedQuantity(item.getPassedQuantity())
                .rejectedQuantity(item.getRejectedQuantity())
                .rejectReason(item.getRejectReason())
                .qcStatus(item.getQcStatus())
                .qcNote(item.getQcNote())
                .inspectedAt(item.getInspectedAt())
                .inspectedBy(item.getInspectedBy() != null ? item.getInspectedBy().getUsername() : null)
                .build();
    }

    public static GoodsReceiptDTO toGoodsReceiptDTO(GoodsReceipt receipt) {
        if (receipt == null) return null;
        return GoodsReceiptDTO.builder()
                .id(receipt.getId())
                .supplierId(receipt.getSupplier() != null ? receipt.getSupplier().getId() : null)
                .supplierName(receipt.getSupplier() != null ? receipt.getSupplier().getName() : null)
                .brandId(receipt.getBrand() != null ? receipt.getBrand().getId() : null)
                .brandName(receipt.getBrand() != null ? receipt.getBrand().getName() : null)
                .totalAmount(receipt.getTotalAmount())
                .note(receipt.getNote())
                .status(receipt.getStatus())
                .createdBy(receipt.getCreatedBy() != null ? receipt.getCreatedBy().getUsername() : null)
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .items(receipt.getItems() != null ? receipt.getItems().stream().map(EntityMapper::toGoodsReceiptItemDTO).collect(Collectors.toList()) : null)
                .build();
    }
}
