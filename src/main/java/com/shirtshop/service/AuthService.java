package com.shirtshop.service;

import com.shirtshop.dto.AuthResponse;
import com.shirtshop.dto.LoginRequest;
import com.shirtshop.dto.RegisterRequest;
import com.shirtshop.entity.User;
import com.shirtshop.exception.ApiException;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService; // ใช้ map User -> UserResponse

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Email or password is incorrect."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ApiException("INVALID_CREDENTIALS", "Email or password is incorrect.");
        }

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)   // ms -> s
                .user(userService.toResponse(user))
                .build();
    }

    public AuthResponse register(RegisterRequest request, String avatarUrl) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("EMAIL_EXISTS", "Email is already registered.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .displayName(request.getDisplayName())
                .phone(request.getPhone())
                .profileImageUrl(avatarUrl) // ✅ เซ็ตรูป
                .emailVerified(false)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)
                .user(userService.toResponse(user)) // หรือใช้ mapper -> UserResponse ถ้ามี
                .build();
    }


    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException("MISSING_REFRESH", "Refresh token is required");
        }

        var jws = jwtService.parse(refreshToken, true); // parse refresh
        String userId = jws.getBody().getSubject();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));

        String newAccess = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(newAccess)
                .refreshToken(refreshToken)               // ส่งตัวเดิมกลับให้
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)   // ms -> s
                .user(userService.toResponse(user))
                .build();
    }
}
