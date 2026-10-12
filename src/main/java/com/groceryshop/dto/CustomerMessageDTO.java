package com.groceryshop.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerMessageDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String senderType; // CUSTOMER | ADMIN
    private String senderName;
    private String message;
    private Boolean isReadByAdmin;
    private Boolean isReadByCustomer;
    private LocalDateTime createdAt;
}
