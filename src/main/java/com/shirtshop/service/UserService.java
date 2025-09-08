package com.shirtshop.service;

import com.shirtshop.dto.AuthResponse;
import com.shirtshop.dto.LoginRequest;
import com.shirtshop.dto.RegisterRequest;
import com.shirtshop.dto.UserResponse;
import com.shirtshop.entity.User;
import com.shirtshop.exception.ApiException;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // มาจาก config bean


    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    public UserResponse register(RegisterRequest req) {
        // ตรวจอีเมลซ้ำ
        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ApiException("EMAIL_ALREADY_USED", "Email is already registered.");
        }
        // ตรวจ username ซ้ำ (ถ้าส่งมา)
        if (StringUtils.hasText(req.getUsername()) &&
                userRepository.existsByUsername(req.getUsername().toLowerCase())) {
            throw new ApiException("USERNAME_ALREADY_USED", "Username is already taken.");
        }

        // เตรียม displayName
        String displayName = req.getDisplayName();
        if (!StringUtils.hasText(displayName)) {
            if (StringUtils.hasText(req.getFirstName()) || StringUtils.hasText(req.getLastName())) {
                displayName = String.format("%s %s",
                        StringUtils.hasText(req.getFirstName()) ? req.getFirstName() : "",
                        StringUtils.hasText(req.getLastName()) ? req.getLastName() : ""
                ).trim();
            } else {
                // fallback จากอีเมล (ก่อน @)
                displayName = req.getEmail().split("@")[0];
            }
        }

        User user = User.builder()
                .email(req.getEmail().toLowerCase())
                .username(StringUtils.hasText(req.getUsername()) ? req.getUsername().toLowerCase() : null)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .profileImageUrl(req.getProfileImageUrl()) // Cloudinary URL จาก FE
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        return toResponse(user);
    }
    public UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .displayName(u.getDisplayName())
                .phone(u.getPhone())
                .profileImageUrl(u.getProfileImageUrl())
                .emailVerified(u.isEmailVerified())
                .build();
    }

    public Optional<User> getById(String id) {
        return userRepository.findById(id);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

}
