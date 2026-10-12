package com.groceryshop.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyDTO {
    private Long userId;
    private String username;
    private String fullName;
    private String currentTier; // BRONZE, SILVER, GOLD, DIAMOND
    private String membershipTier; // alias for currentTier
    private Integer loyaltyPoints;
    private String nextTier;
    private Integer pointsToNextTier;
    private Double progressPercentage;
    private Double tierDiscountPercent;
    private String tierName;
    private String tierBadge;
    private List<String> benefits;
    private List<String> currentTierBenefits;
    private List<TierInfoDTO> allTiers;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TierInfoDTO {
        private String tierKey;
        private String tierName;
        private Integer minPoints;
        private Integer discountPercent;
        private List<String> benefits;
        private Boolean isCurrent;
    }
}

