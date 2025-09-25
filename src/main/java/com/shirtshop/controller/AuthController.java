package com.shirtshop.controller;

import com.shirtshop.dto.*;
import com.shirtshop.entity.User;
import com.shirtshop.service.AuthService;
import com.shirtshop.service.UserService;
import com.shirtshop.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final CloudinaryService cloudinaryService;
    // --- เมธอดจาก UserController เดิม ---
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestPart("request") RegisterRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {

        String avatarUrl = null;
        if (avatar != null && !avatar.isEmpty()) {
            // ✅ ดึง url ออกมาก่อน (แก้ incompatible types)
            CloudinaryUploadResponse uploaded = cloudinaryService.uploadFile(avatar, "shirtshop/avatars");
            avatarUrl = uploaded.getUrl();
        }

        return ResponseEntity.ok(authService.register(request, avatarUrl));
    }



    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refresh(body.get("refreshToken")));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() { // เปลี่ยน Response Type เป็น UserResponse
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // ดึง User entity ที่ผ่านการ authenticate แล้วออกมา
        var userPrincipal = (com.shirtshop.entity.User) authentication.getPrincipal();

        // ⭐️ ใช้ toResponse เพื่อแปลงเป็น DTO ที่สมบูรณ์
        UserResponse response = userService.toResponse(userPrincipal);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me") // ใช้ PUT สำหรับการอัปเดต
    public ResponseEntity<UserResponse> updateUserProfile(
            @RequestBody UpdateUserRequest request) { // สร้าง DTO ใหม่สำหรับรับข้อมูล

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userPrincipal = (com.shirtshop.entity.User) authentication.getPrincipal();
        String userId = userPrincipal.getId();

        // เรียก Service มาอัปเดตข้อมูล
        User updatedUser = userService.updateUserProfile(userId, request);

        return ResponseEntity.ok(userService.toResponse(updatedUser));
    }

}