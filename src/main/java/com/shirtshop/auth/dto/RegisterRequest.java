package com.shirtshop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
public class RegisterRequest {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phone;

    // จากขั้นตอนอัปโหลด Cloudinary ฝั่ง FE
    private String avatarUrl;       // optional
    private String avatarPublicId;  // optional  ✅
}
