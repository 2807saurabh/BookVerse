package com.example.ecom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class LoginResponse {

    private String token;
    private String role;

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }
}