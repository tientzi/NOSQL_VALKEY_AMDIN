package com.groceryshop.controller;

import com.groceryshop.dto.ProfileUpdateRequest;
import com.groceryshop.dto.UserDTO;
import com.groceryshop.security.UserPrincipal;
import com.groceryshop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getUserProfile(userPrincipal.getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(userPrincipal.getId(), request));
    }

    @GetMapping("/loyalty-info")
    public ResponseEntity<com.groceryshop.dto.LoyaltyDTO> getLoyaltyInfo(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getLoyaltyInfo(userPrincipal.getId()));
    }
}
