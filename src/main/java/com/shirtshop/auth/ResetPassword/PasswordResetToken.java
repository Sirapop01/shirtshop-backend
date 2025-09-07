package com.shirtshop.auth.ResetPassword;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document("password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    private String email;
    private String otp;           // โค้ด OTP 6 หลัก
    private Instant expiresAt;    // วันหมดอายุ
}
