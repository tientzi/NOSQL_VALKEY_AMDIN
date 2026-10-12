package com.groceryshop.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String roleName;
    private Boolean isActive;
    private Integer loyaltyPoints;
    private String membershipTier;
    private String provider;
    private String avatarUrl;
    private LocalDateTime createdAt;

    // Chi tiết cấp bậc thành viên & quyền lợi
    private LoyaltyDTO loyaltyInfo;

    // Sổ địa chỉ nhận hàng của khách
    private List<AddressDTO> addresses;

    // Thống kê đơn hàng
    private Long totalOrders;
    private Double totalSpent;
    private Long completedOrders;
    private Long cancelledOrders;

    // 10 đơn hàng gần đây nhất
    private List<OrderDTO> recentOrders;
}
