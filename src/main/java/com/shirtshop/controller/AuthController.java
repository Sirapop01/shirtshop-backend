package com.shirtshop.controller;

import com.shirtshop.dto.*;
import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import com.shirtshop.service.AuthService;
import com.shirtshop.service.UserService;
import com.shirtshop.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final UserRepository userRepository;

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
    public ResponseEntity<UserResponse> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Object principal = auth.getPrincipal();
        User u;

        try {
            if (principal instanceof com.shirtshop.entity.User) {
                u = (com.shirtshop.entity.User) principal; // กรณี JwtAuthenticationFilter ตั้งเป็น User ให้แล้ว
            } else if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
                // กรณีเป็น UserDetails ของ Spring → username มักเป็นอีเมล
                u = userService.findByEmail(springUser.getUsername());
            } else if (principal instanceof String s) {
                // อาจเป็น "anonymousUser", userId หรือ email
                if ("anonymousUser".equalsIgnoreCase(s)) {
                    return ResponseEntity.status(401).build();
                }
                if (s.contains("@")) {
                    u = userService.findByEmail(s);
                } else {
                    u = userService.findByIdOrThrow(s);
                }
            } else {
                return ResponseEntity.status(401).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userService.toResponse(u));
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

    @PostMapping("/password/otp")
    public ResponseEntity<Map<String, Object>> sendPasswordOtp(@RequestBody OtpRequest req) {
        authService.sendPasswordOtp(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordRequest req) {
        authService.resetPasswordWithOtp(req.getEmail(), req.getOtp(), req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset success"));
    }

    // ====== เปลี่ยนรหัสผ่าน (ต้องล็อกอิน) ======
    @PostMapping("/password/change")
    public ResponseEntity<?> changePasswordPost(@Valid @RequestBody ChangePasswordRequest req) {
        return changePasswordInternal(req);
    }

    // เผื่อฝั่ง FE บางหน้าส่งเป็น PUT — รองรับไว้ด้วย
    @PutMapping("/password/change")
    public ResponseEntity<?> changePasswordPut(@Valid @RequestBody ChangePasswordRequest req) {
        return changePasswordInternal(req);
    }

    private ResponseEntity<?> changePasswordInternal(ChangePasswordRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        // principal อาจเป็น User ของเรา หรือเป็น UserDetails/String ก็ได้
        String principalKey;
        Object principal = auth.getPrincipal();
        if (principal instanceof User u) {
            principalKey = (u.getEmail() != null && !u.getEmail().isBlank()) ? u.getEmail() : u.getId();
        } else {
            principalKey = auth.getName(); // บางระบบ map เป็น userId/username/email
        }

        authService.changePasswordByPrincipal(principalKey, req);
        return ResponseEntity.ok().build();
    }

}