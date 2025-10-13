package com.shirtshop.service;

import com.shirtshop.dto.AuthResponse;
import com.shirtshop.dto.LoginRequest;
import com.shirtshop.dto.RegisterRequest;
import com.shirtshop.dto.UserResponse;
import com.shirtshop.entity.User;
import com.shirtshop.exception.ApiException;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    @Value("${app.jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    /** ============ LOGIN ============ */
    public AuthResponse login(LoginRequest req) {
        final String email = req.getEmail() == null ? "" : req.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Email or password is incorrect."));

        String storedHash = user.getPasswordHash();
        if (!StringUtils.hasText(storedHash) || !passwordEncoder.matches(req.getPassword(), storedHash)) {
            throw new ApiException("INVALID_CREDENTIALS", "Email or password is incorrect.");
        }

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        userService.markActiveById(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)
                .user(userService.toResponse(user))
                .build();
    }

    /** ============ REGISTER ============ */
    public AuthResponse register(RegisterRequest req, String avatarUrl) {
        final String email = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("EMAIL_ALREADY_USED", "This email is already registered.");
        }

        // username default = email หน้า @
        String username = req.getUsername();
        if (!StringUtils.hasText(username)) {
            username = email.split("@")[0];
        }

        if (userRepository.existsByUsername(username)) {
            throw new ApiException("USERNAME_ALREADY_USED", "This username is already taken.");
        }

        User user = User.builder()
                .email(email)
                .username(username.trim().toLowerCase())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .displayName(req.getDisplayName())
                .passwordHash(passwordEncoder.encode(req.getPassword())) // ✅ ใช้ passwordHash
                .phone(req.getPhone())
                .profileImageUrl(avatarUrl)
                .emailVerified(false)
                .active(false)
                .build();

        user = userRepository.save(user);

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        userService.markActiveById(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)
                .user(userService.toResponse(user))
                .build();
    }

    /** ============ REFRESH TOKEN ============ */
    public AuthResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !jwtService.validateToken(refreshToken)) {
            throw new ApiException("INVALID_TOKEN", "Refresh token is invalid or expired.");
        }

        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found."));

        String newAccessToken  = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        userService.markActiveById(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpirationMs / 1000L)
                .user(userService.toResponse(user))
                .build();
    }
}
