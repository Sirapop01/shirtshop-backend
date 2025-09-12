package com.shirtshop.controller;

import com.shirtshop.dto.*;
import com.shirtshop.entity.User;
import com.shirtshop.service.AuthService;
import com.shirtshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth") // ใช้ Path นี้เป็นหลัก
@CrossOrigin
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    // --- เมธอดจาก UserController เดิม ---
    @PostMapping(value = "/register", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<UserResponse> register(
            @Valid @RequestPart("user") RegisterRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {

        UserResponse created = userService.register(request, profileImage);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refresh(body.get("refreshToken")));
    }

    // --- เมธอด /me ที่ปรับปรุงให้ดีขึ้น ---
    /**
     * Endpoint สำหรับดึงข้อมูลของผู้ใช้ที่กำลังล็อกอินอยู่ (Profile)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails currentUser) {
        // ใช้ @AuthenticationPrincipal ทำให้โค้ดสั้นและปลอดภัยขึ้น
        User user = userService.findByEmail(currentUser.getUsername());
        return ResponseEntity.ok(userService.toResponse(user));
    }
}