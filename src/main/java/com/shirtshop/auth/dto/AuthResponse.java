package com.shirtshop.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;   // "Bearer" token
    private String tokenType;     // "Bearer"
    private long   expiresIn;     // วินาทีของ access
    private String refreshToken;
}
