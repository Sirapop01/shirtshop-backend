package com.shirtshop.auth;

import com.shirtshop.auth.dto.AuthResponse;
import com.shirtshop.auth.dto.LoginRequest;
import com.shirtshop.auth.dto.RegisterRequest;
import com.shirtshop.auth.dto.ForgotPasswordRequest;
import com.shirtshop.auth.dto.ResetPasswordRequest;
import com.shirtshop.auth.ResetPassword.PasswordResetService;
import com.shirtshop.security.JwtService;
import com.shirtshop.user.Address;
import com.shirtshop.user.User;
import com.shirtshop.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;



import java.util.*;


@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwt;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody RegisterRequest req) {
        var u = userService.registerLocal(req);

        String roles = String.join(",", u.getRoles().stream().map(Enum::name).toList());
        String access  = jwt.genAccess(u.getId(), u.getEmail(), roles);
        String refresh = jwt.genRefresh(u.getId());

        var def = (u.getAddresses() == null || u.getAddresses().isEmpty())
                ? null
                : u.getAddresses().get(0);

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", u.getId());
        userMap.put("email", u.getEmail());
        userMap.put("name", u.getDisplayName());
        userMap.put("firstName", u.getFirstName());
        userMap.put("lastName", u.getLastName());
        userMap.put("phone", u.getPhone());
        userMap.put("avatarUrl", u.getAvatarUrl());
        userMap.put("defaultShipping", def);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("accessToken", access);
        resp.put("refreshToken", refresh);
        resp.put("user", userMap);

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam("token") String refreshToken) {
        try {
            var claims = jwt.parseRefresh(refreshToken);
            var userId = claims.getSubject();

            var user = userService.getByIdOrThrow(userId);
            String roles = String.join(",", user.getRoles().stream().map(Enum::name).toList());
            String access = jwt.genAccess(user.getId(), user.getEmail(), roles);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("accessToken", access);
            resp.put("tokenType", "Bearer");
            resp.put("expiresIn", 60L * 15);
            resp.put("refreshToken", refreshToken);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }



    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User u = userService.validateLogin(req.getEmail(), req.getPassword());
        String roles = String.join(",", u.getRoles().stream().map(Enum::name).toList());

        String access  = jwt.genAccess(u.getId(), u.getEmail(), roles);
        String refresh = jwt.genRefresh(u.getId());

        return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "refreshToken", refresh,
                "user", Map.of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "name", u.getDisplayName(),
                        "roles", u.getRoles()
                )
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        var user = userService.getByIdOrThrow(auth.getName());
        return ResponseEntity.ok(java.util.Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "roles", user.getRoles()
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        if (!userService.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Email not found"));
        }
        String otp = passwordResetService.createOtpForEmail(req.getEmail());
        // TODO: ส่งอีเมลจริง
        System.out.println("OTP for " + req.getEmail() + " = " + otp);
        return ResponseEntity.ok(java.util.Map.of("message", "OTP sent to email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        if (!passwordResetService.validateOtp(req.getEmail(), req.getOtp())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Invalid or expired OTP"));
        }
        userService.updatePassword(req.getEmail(), req.getNewPassword());
        return ResponseEntity.ok(java.util.Map.of("message", "Password reset successful"));
    }


}
