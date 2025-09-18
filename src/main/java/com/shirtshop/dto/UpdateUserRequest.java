package com.shirtshop.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO สำหรับรับข้อมูลตอนอัปเดต Profile
@Getter
@Setter
@NoArgsConstructor
public class UpdateUserRequest {
    // รับแค่ field ที่อนุญาตให้อัปเดตจากหน้าโปรไฟล์
    private String firstName;
    private String lastName;
    private String phone;
}