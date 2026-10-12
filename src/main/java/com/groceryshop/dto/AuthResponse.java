package com.groceryshop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private Long id;
    private String username;
    private String email;
    private String role;

    public AuthResponse(String accessToken, Long id, String username, String email, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
