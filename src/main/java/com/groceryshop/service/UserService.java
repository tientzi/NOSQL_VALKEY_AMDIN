package com.groceryshop.service;

import com.groceryshop.dto.RegisterRequest;
import com.groceryshop.dto.UserDTO;
import com.groceryshop.dto.ProfileUpdateRequest;
import com.groceryshop.dto.UserDetailDTO;
import com.groceryshop.dto.AddressDTO;
import com.groceryshop.dto.OrderDTO;
import com.groceryshop.dto.LoyaltyDTO;
import com.groceryshop.entity.Order;
import com.groceryshop.entity.Role;
import com.groceryshop.entity.User;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.RoleRepository;
import com.groceryshop.repository.UserRepository;
import com.groceryshop.repository.AddressRepository;
import com.groceryshop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Transactional
    public UserDTO registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username đã được sử dụng");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Role ROLE_USER trong hệ thống"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(userRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        return EntityMapper.toUserDTO(savedUser);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(EntityMapper::toUserDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        if (user.getUsername().equals("admin")) {
            throw new BadRequestException("Không thể khóa tài khoản admin hệ thống chính");
        }
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    public UserDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        UserDTO dto = EntityMapper.toUserDTO(user);
        // Tự động làm sạch phone nếu bị lưu nhầm là email
        if (dto.getPhone() != null && dto.getPhone().contains("@")) {
            dto.setPhone(null);
        }
        return dto;
    }

    @Transactional
    public UserDTO updateUserProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            // Không cho phép lưu email vào trường phone
            if (!phone.contains("@")) {
                user.setPhone(phone.isEmpty() ? null : phone);
            }
        }
        // Tự động fix dữ liệu phone sai (email bị lưu nhầm vào phone)
        if (user.getPhone() != null && user.getPhone().contains("@")) {
            user.setPhone(null);
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(request.getPassword().trim());
        }

        User updatedUser = userRepository.save(user);
        return EntityMapper.toUserDTO(updatedUser);
    }

    public com.groceryshop.dto.LoyaltyDTO getLoyaltyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        int points = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        String tier = user.getMembershipTier() != null ? user.getMembershipTier().toUpperCase() : "BRONZE";
        double discountPercent = 0.0;
        String nextTier = "SILVER";
        int pointsNeeded = 100 - points;
        double progress = Math.min(100.0, (points / 100.0) * 100.0);
        String tierName = "Đồng";
        String tierBadge = "🥉";

        if (points >= 1000 || "DIAMOND".equals(tier)) {
            tier = "DIAMOND";
            tierName = "Kim Cương";
            tierBadge = "💎";
            discountPercent = 8.0;
            nextTier = null;
            pointsNeeded = 0;
            progress = 100.0;
        } else if (points >= 500 || "GOLD".equals(tier)) {
            tier = "GOLD";
            tierName = "Vàng";
            tierBadge = "🥇";
            discountPercent = 5.0;
            nextTier = "DIAMOND";
            pointsNeeded = Math.max(0, 1000 - points);
            progress = Math.min(100.0, Math.max(0.0, ((points - 500) / 500.0) * 100.0));
        } else if (points >= 100 || "SILVER".equals(tier)) {
            tier = "SILVER";
            tierName = "Bạc";
            tierBadge = "🥈";
            discountPercent = 2.0;
            nextTier = "GOLD";
            pointsNeeded = Math.max(0, 500 - points);
            progress = Math.min(100.0, Math.max(0.0, ((points - 100) / 400.0) * 100.0));
        } else {
            tier = "BRONZE";
            tierName = "Đồng";
            tierBadge = "🥉";
            discountPercent = 0.0;
            nextTier = "SILVER";
            pointsNeeded = Math.max(0, 100 - points);
            progress = Math.min(100.0, Math.max(0.0, (points / 100.0) * 100.0));
        }

        if (!tier.equalsIgnoreCase(user.getMembershipTier())) {
            user.setMembershipTier(tier);
            userRepository.save(user);
        }

        final String currentTierCode = tier;
        List<com.groceryshop.dto.LoyaltyDTO.TierInfoDTO> allTiers = List.of(
                com.groceryshop.dto.LoyaltyDTO.TierInfoDTO.builder()
                        .tierKey("BRONZE")
                        .tierName("Đồng (Bronze)")
                        .minPoints(0)
                        .discountPercent(0)
                        .benefits(List.of("Tích lũy 1% giá trị đơn hàng (1 điểm mỗi 100.000đ)", "Áp dụng các mã voucher khuyến mãi công khai"))
                        .isCurrent("BRONZE".equals(currentTierCode))
                        .build(),
                com.groceryshop.dto.LoyaltyDTO.TierInfoDTO.builder()
                        .tierKey("SILVER")
                        .tierName("Bạc (Silver)")
                        .minPoints(100)
                        .discountPercent(2)
                        .benefits(List.of("Tích lũy 1% + Giảm ngay 2% trên mọi đơn hàng", "Quà tặng sinh nhật thành viên MiniMart", "Ưu tiên nhận thông báo flash sale"))
                        .isCurrent("SILVER".equals(currentTierCode))
                        .build(),
                com.groceryshop.dto.LoyaltyDTO.TierInfoDTO.builder()
                        .tierKey("GOLD")
                        .tierName("Vàng (Gold)")
                        .minPoints(500)
                        .discountPercent(5)
                        .benefits(List.of("Tích lũy 1.5% + Giảm ngay 5% trên mọi đơn hàng", "Giảm 30% phí vận chuyển cho mọi đơn", "Ưu tiên đóng gói và chuẩn bị hàng siêu tốc", "Voucher tri ân định kỳ hàng tháng"))
                        .isCurrent("GOLD".equals(currentTierCode))
                        .build(),
                com.groceryshop.dto.LoyaltyDTO.TierInfoDTO.builder()
                        .tierKey("DIAMOND")
                        .tierName("Kim Cương (Diamond)")
                        .minPoints(1000)
                        .discountPercent(8)
                        .benefits(List.of("Tích lũy 2% + Giảm ngay 8% trên mọi đơn hàng", "Miễn phí vận chuyển 100% cho mọi đơn", "Đường dây nóng hỗ trợ VIP 24/7", "Quà tri ân đặc quyền cuối năm"))
                        .isCurrent("DIAMOND".equals(currentTierCode))
                        .build()
        );

        List<String> currentBenefits = allTiers.stream()
                .filter(t -> t.getTierKey().equals(currentTierCode))
                .findFirst()
                .map(com.groceryshop.dto.LoyaltyDTO.TierInfoDTO::getBenefits)
                .orElse(List.of());

        return com.groceryshop.dto.LoyaltyDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                .currentTier(tier)
                .membershipTier(tier)
                .loyaltyPoints(points)
                .tierDiscountPercent(discountPercent)
                .nextTier(nextTier)
                .pointsToNextTier(pointsNeeded)
                .progressPercentage(Math.round(progress * 10.0) / 10.0)
                .tierName(tierName)
                .tierBadge(tierBadge)
                .benefits(currentBenefits)
                .currentTierBenefits(currentBenefits)
                .allTiers(allTiers)
                .build();
    }

    public UserDetailDTO getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        // 1. Thông tin Loyalty Tier
        LoyaltyDTO loyaltyInfo = getLoyaltyInfo(userId);

        // 2. Danh sách địa chỉ nhận hàng
        List<AddressDTO> addresses = addressRepository.findByUserId(userId).stream()
                .map(EntityMapper::toAddressDTO)
                .collect(Collectors.toList());

        // 3. Lịch sử và thống kê đơn hàng
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus()) 
                        || "HOAN_THANH".equalsIgnoreCase(o.getStatus())
                        || "DA_GIAO".equalsIgnoreCase(o.getStatus()) 
                        || "DA_THANH_TOAN".equalsIgnoreCase(o.getStatus()))
                .count();
        long cancelledOrders = orders.stream()
                .filter(o -> "CANCELLED".equalsIgnoreCase(o.getStatus()) 
                        || "HUY".equalsIgnoreCase(o.getStatus())
                        || "DA_HUY".equalsIgnoreCase(o.getStatus()))
                .count();
        double totalSpent = orders.stream()
                .filter(o -> !"CANCELLED".equalsIgnoreCase(o.getStatus()) 
                        && !"HUY".equalsIgnoreCase(o.getStatus())
                        && !"DA_HUY".equalsIgnoreCase(o.getStatus()) 
                        && (o.getFinalAmount() != null || o.getTotalAmount() != null))
                .mapToDouble(o -> o.getFinalAmount() != null ? o.getFinalAmount().doubleValue() : o.getTotalAmount().doubleValue())
                .sum();

        List<OrderDTO> recentOrders = orders.stream()
                .limit(10)
                .map(EntityMapper::toOrderDTO)
                .collect(Collectors.toList());

        return UserDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roleName(user.getRole() != null ? user.getRole().getName() : "ROLE_USER")
                .isActive(user.getIsActive())
                .loyaltyPoints(user.getLoyaltyPoints())
                .membershipTier(user.getMembershipTier())
                .provider(user.getProvider())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .loyaltyInfo(loyaltyInfo)
                .addresses(addresses)
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .recentOrders(recentOrders)
                .build();
    }
}

