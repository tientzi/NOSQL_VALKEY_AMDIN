package com.groceryshop.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerConversationDTO {
    private Long userId;
    private String userName;
    private String fullName;
    private String userEmail;
    private String userPhone;
    private String membershipTier;
    private String lastMessage;
    private String lastSenderType;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
}
