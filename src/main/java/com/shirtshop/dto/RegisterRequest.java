package com.shirtshop.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters.")
    private String password;

    // ไม่บังคับ แต่ถ้าอยากบังคับก็ใส่ @NotBlank ได้
    private String firstName;
    private String lastName;

    // ถ้า FE ให้ผู้ใช้ตั้งชื่อแสดงผลเอง ก็ส่งมา; ไม่มาก็ไป derive จาก firstName/lastName
    private String displayName;

    // optional
    private String username; // ถ้าไม่ใช้ก็ไม่ต้องส่ง
    private String phone;

    // FE อัปโหลด Cloudinary ก่อน แล้วส่งลิงก์มาเก็บ
    private String profileImageUrl;
}
