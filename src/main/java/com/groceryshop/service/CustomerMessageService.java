package com.groceryshop.service;

import com.groceryshop.dto.CustomerConversationDTO;
import com.groceryshop.dto.CustomerMessageDTO;
import com.groceryshop.entity.CustomerMessage;
import com.groceryshop.entity.User;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.repository.CustomerMessageRepository;
import com.groceryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerMessageService {

    @Autowired
    private CustomerMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    private CustomerMessageDTO toDTO(CustomerMessage msg) {
        return CustomerMessageDTO.builder()
                .id(msg.getId())
                .userId(msg.getUser().getId())
                .userName(msg.getUser().getUsername())
                .userEmail(msg.getUser().getEmail())
                .senderType(msg.getSenderType())
                .senderName(msg.getSenderName())
                .message(msg.getMessage())
                .isReadByAdmin(msg.getIsReadByAdmin())
                .isReadByCustomer(msg.getIsReadByCustomer())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    // ================= KHÁCH HÀNG =================
    @Transactional
    public List<CustomerMessageDTO> getCustomerMessages(Long userId) {
        List<CustomerMessage> messages = messageRepository.findByUserIdOrderByCreatedAtAsc(userId);
        
        // Đánh dấu các tin Admin đã gửi là khách hàng đã đọc
        boolean hasUnread = false;
        for (CustomerMessage msg : messages) {
            if ("ADMIN".equalsIgnoreCase(msg.getSenderType()) && Boolean.FALSE.equals(msg.getIsReadByCustomer())) {
                msg.setIsReadByCustomer(true);
                hasUnread = true;
            }
        }
        if (hasUnread) {
            messageRepository.saveAll(messages);
        }

        return messages.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public CustomerMessageDTO sendCustomerMessage(Long userId, String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new BadRequestException("Nội dung tin nhắn không được để trống");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        String senderName = (user.getFullName() != null && !user.getFullName().isBlank()) 
                ? user.getFullName() 
                : user.getUsername();

        CustomerMessage message = CustomerMessage.builder()
                .user(user)
                .senderType("CUSTOMER")
                .senderName(senderName)
                .message(messageContent.trim())
                .isReadByAdmin(false)
                .isReadByCustomer(true)
                .build();

        CustomerMessage saved = messageRepository.save(message);
        return toDTO(saved);
    }

    // ================= ADMIN =================
    public List<CustomerConversationDTO> getAllConversations() {
        List<Long> userIds = messageRepository.findDistinctUserIds();
        List<CustomerConversationDTO> conversations = new ArrayList<>();

        for (Long uId : userIds) {
            userRepository.findById(uId).ifPresent(user -> {
                List<CustomerMessage> userMsgs = messageRepository.findByUserIdOrderByCreatedAtAsc(uId);
                if (!userMsgs.isEmpty()) {
                    CustomerMessage lastMsg = userMsgs.get(userMsgs.size() - 1);
                    long unreadCount = messageRepository.countByUserIdAndSenderTypeAndIsReadByAdminFalse(uId, "CUSTOMER");

                    conversations.add(CustomerConversationDTO.builder()
                            .userId(user.getId())
                            .userName(user.getUsername())
                            .fullName(user.getFullName() != null ? user.getFullName() : user.getUsername())
                            .userEmail(user.getEmail())
                            .userPhone(user.getPhone())
                            .membershipTier(user.getMembershipTier())
                            .lastMessage(lastMsg.getMessage())
                            .lastSenderType(lastMsg.getSenderType())
                            .lastMessageTime(lastMsg.getCreatedAt())
                            .unreadCount(unreadCount)
                            .build());
                }
            });
        }

        // Sắp xếp hội thoại có tin nhắn mới nhất lên đầu
        conversations.sort((a, b) -> {
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return conversations;
    }

    @Transactional
    public List<CustomerMessageDTO> getAdminConversation(Long targetUserId) {
        List<CustomerMessage> messages = messageRepository.findByUserIdOrderByCreatedAtAsc(targetUserId);

        // Đánh dấu toàn bộ tin do khách gửi là Admin đã đọc
        boolean hasUnread = false;
        for (CustomerMessage msg : messages) {
            if ("CUSTOMER".equalsIgnoreCase(msg.getSenderType()) && Boolean.FALSE.equals(msg.getIsReadByAdmin())) {
                msg.setIsReadByAdmin(true);
                hasUnread = true;
            }
        }
        if (hasUnread) {
            messageRepository.saveAll(messages);
        }

        return messages.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public CustomerMessageDTO sendAdminReply(Long adminUserId, Long targetUserId, String messageContent) {
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new BadRequestException("Nội dung phản hồi không được để trống");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + targetUserId));

        User admin = userRepository.findById(adminUserId).orElse(null);
        String adminName = "Hỗ trợ Khách hàng MiniMart";
        if (admin != null && admin.getFullName() != null && !admin.getFullName().isBlank()) {
            adminName = "Hỗ trợ MiniMart (" + admin.getFullName() + ")";
        }

        CustomerMessage message = CustomerMessage.builder()
                .user(targetUser)
                .senderType("ADMIN")
                .senderName(adminName)
                .message(messageContent.trim())
                .isReadByAdmin(true)
                .isReadByCustomer(false)
                .build();

        CustomerMessage saved = messageRepository.save(message);
        return toDTO(saved);
    }

    public long getUnreadAdminCount() {
        return messageRepository.countBySenderTypeAndIsReadByAdminFalse("CUSTOMER");
    }
}
