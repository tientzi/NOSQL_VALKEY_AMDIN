package com.groceryshop.service;

import com.groceryshop.dto.CouponDTO;
import com.groceryshop.dto.CouponItemInfo;
import com.groceryshop.entity.Category;
import com.groceryshop.entity.Coupon;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.CategoryRepository;
import com.groceryshop.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    public List<CouponDTO> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(EntityMapper::toCouponDTO)
                .collect(Collectors.toList());
    }

    public List<CouponDTO> getActivePublicCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> c.getStartDate() == null || !c.getStartDate().isAfter(now))
                .filter(c -> c.getEndDate() == null || !c.getEndDate().isBefore(now))
                .filter(c -> c.getMaxUses() == null || c.getUsedCount() == null || c.getUsedCount() < c.getMaxUses())
                .map(EntityMapper::toCouponDTO)
                .collect(Collectors.toList());
    }

    public CouponDTO getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy coupon với id: " + id));
        return EntityMapper.toCouponDTO(coupon);
    }

    public CouponDTO getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy coupon với mã: " + code));
        return EntityMapper.toCouponDTO(coupon);
    }

    @Transactional
    public CouponDTO createCoupon(CouponDTO dto) {
        if (couponRepository.findByCode(dto.getCode()).isPresent()) {
            throw new BadRequestException("Mã coupon đã tồn tại");
        }
        Coupon coupon = EntityMapper.toCouponEntity(dto);
        if (dto.getApplicableCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getApplicableCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục áp dụng không tồn tại"));
            coupon.setApplicableCategory(category);
        }
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());

        Coupon saved = couponRepository.save(coupon);
        return EntityMapper.toCouponDTO(saved);
    }

    @Transactional
    public CouponDTO updateCoupon(Long id, CouponDTO dto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy coupon với id: " + id));

        couponRepository.findByCode(dto.getCode())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Mã coupon đã được sử dụng");
                    }
                });

        coupon.setCode(dto.getCode());
        coupon.setDescription(dto.getDescription());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setStartDate(dto.getStartDate());
        coupon.setEndDate(dto.getEndDate());
        coupon.setMinOrderAmount(dto.getMinOrderAmount());
        coupon.setMaxUses(dto.getMaxUses());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());

        if (dto.getApplicableCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getApplicableCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục áp dụng không tồn tại"));
            coupon.setApplicableCategory(category);
        } else {
            coupon.setApplicableCategory(null);
        }

        if (dto.getIsActive() != null) {
            coupon.setIsActive(dto.getIsActive());
        }

        Coupon updated = couponRepository.save(coupon);
        return EntityMapper.toCouponDTO(updated);
    }

    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy coupon với id: " + id));
        couponRepository.delete(coupon);
    }

    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        return calculateDiscountForItems(code, orderAmount, null);
    }

    public BigDecimal calculateDiscountForItems(String code, BigDecimal orderAmount, List<CouponItemInfo> items) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BadRequestException("Mã giảm giá này hiện không hoạt động");
        }
        if (now.isBefore(coupon.getStartDate())) {
            throw new BadRequestException("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (now.isAfter(coupon.getEndDate())) {
            throw new BadRequestException("Mã giảm giá đã hết hạn sử dụng");
        }
        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new BadRequestException("Mã giảm giá đã đạt số lượt sử dụng tối đa");
        }

        BigDecimal baseAmount = orderAmount;

        // Kiểm tra phạm vi áp dụng theo danh mục (nếu có)
        if (coupon.getApplicableCategory() != null) {
            Long targetCatId = coupon.getApplicableCategory().getId();
            String catName = coupon.getApplicableCategory().getName();

            if (items == null || items.isEmpty()) {
                throw new BadRequestException("Mã '" + coupon.getCode() + "' chỉ áp dụng cho danh mục '" + catName + "'. Vui lòng kiểm tra lại sản phẩm trong giỏ hàng.");
            }

            List<CouponItemInfo> matchingItems = items.stream()
                    .filter(i -> i.getCategoryId() != null && i.getCategoryId().equals(targetCatId))
                    .collect(Collectors.toList());

            if (matchingItems.isEmpty()) {
                throw new BadRequestException("Đơn hàng chưa có sản phẩm nào thuộc danh mục '" + catName + "' để áp dụng mã giảm giá này.");
            }

            BigDecimal categoryTotal = matchingItems.stream()
                    .map(i -> (i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO)
                            .multiply(BigDecimal.valueOf(i.getQuantity() != null ? i.getQuantity() : 1)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (coupon.getMinOrderAmount() != null && categoryTotal.compareTo(coupon.getMinOrderAmount()) < 0) {
                throw new BadRequestException("Tổng tiền các sản phẩm thuộc danh mục '" + catName + "' phải đạt tối thiểu "
                        + coupon.getMinOrderAmount().longValue() + "đ (Hiện tại: " + categoryTotal.longValue() + "đ)");
            }

            baseAmount = categoryTotal;
        } else {
            // Không giới hạn danh mục: kiểm tra giá trị đơn hàng chung
            if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                throw new BadRequestException("Giá trị đơn hàng chưa đạt tối thiểu (" + coupon.getMinOrderAmount().longValue() + "đ) để áp dụng mã này");
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = baseAmount.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            // Cắt trần giảm giá tối đa (nếu có cấu hình)
            if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                    discount = coupon.getMaxDiscountAmount();
                }
            }
        } else if ("FIXED_AMOUNT".equalsIgnoreCase(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        }

        // Không được giảm quá tổng tiền có thể giảm
        if (discount.compareTo(baseAmount) > 0) {
            discount = baseAmount;
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }
}
