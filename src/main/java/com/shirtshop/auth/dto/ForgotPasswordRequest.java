// src/main/java/com/shirtshop/auth/dto/ForgotPasswordRequest.java
package com.shirtshop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ForgotPasswordRequest {
    @Email @NotBlank
    private String email;
}
