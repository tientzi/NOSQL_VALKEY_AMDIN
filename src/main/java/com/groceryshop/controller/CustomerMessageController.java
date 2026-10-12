package com.groceryshop.controller;

import com.groceryshop.dto.CustomerConversationDTO;
import com.groceryshop.dto.CustomerMessageDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.CustomerMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class CustomerMessageController {

    @Autowired
    private CustomerMessageService messageService;

    // ================= KHÁCH HÀNG (Client) =================
    @GetMapping("/api/messages/my-messages")
    public ResponseEntity<?> getMyMessages(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để xem tin nhắn"));
        }
        return ResponseEntity.ok(messageService.getCustomerMessages(userPrincipal.getId()));
    }

    @PostMapping("/api/messages/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody Map<String, String> payload) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập để gửi tin nhắn"));
        }
        String content = payload.get("message");
        return ResponseEntity.ok(messageService.sendCustomerMessage(userPrincipal.getId(), content));
    }

    // ================= ADMIN =================
    @GetMapping("/api/admin/messages/conversations")
    public ResponseEntity<List<CustomerConversationDTO>> getAllConversations() {
        return ResponseEntity.ok(messageService.getAllConversations());
    }

    @GetMapping("/api/admin/messages/conversations/{userId}")
    public ResponseEntity<List<CustomerMessageDTO>> getConversationMessages(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getAdminConversation(userId));
    }

    @PostMapping("/api/admin/messages/conversations/{userId}/reply")
    public ResponseEntity<CustomerMessageDTO> replyToCustomer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload) {
        String content = payload.get("message");
        return ResponseEntity.ok(messageService.sendAdminReply(userPrincipal.getId(), userId, content));
    }

    @GetMapping("/api/admin/messages/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", messageService.getUnreadAdminCount()));
    }
}
