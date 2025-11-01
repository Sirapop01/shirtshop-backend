package com.shirtshop.service;

import com.shirtshop.dto.AuthResponse;
import com.shirtshop.dto.LoginRequest;
import com.shirtshop.dto.RegisterRequest;
import com.shirtshop.dto.ChangePasswordRequest;
import com.shirtshop.entity.User;
import com.shirtshop.exception.ApiException;
import com.shirtshop.repository.UserRepository;
import com.shirtshop.util.EmailSender;
import com.shirtshop.util.OtpStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpStore otpStore;

    private final EmailSender emailSender;


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

    /** ส่ง OTP: ตอบ 200 เสมอ เพื่อลด user-enumeration */
    public void sendPasswordOtp(String email) {
        Optional<User> maybe = userRepository.findByEmail(email);
        if (maybe.isEmpty()) {
            log.info("[sendPasswordOtp] email not found -> return 200 to avoid enumeration: {}", email);
            // ไม่ทำอะไรต่อ แต่ตอบ 200 จาก Controller
            return;
        }

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        otpStore.save(email, otp, Duration.ofMinutes(10));

        try {
            emailSender.send(email, "Your ShirtShop OTP",
                    "Your OTP is: " + otp + " (valid 10 minutes)");
        } catch (Exception ex) {
            // ป้องกัน 500 กรณีระบบอีเมลล่ม — เรา log และยังตอบ 200
            log.error("[sendPasswordOtp] failed to send email to {}: {}", email, ex.getMessage(), ex);
        }
    }

    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        if (!otpStore.verify(email, otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded); // ต้องมี field password + @Setter ใน User
        userRepository.save(user);

        otpStore.consume(email);
    }
    // ====== เปลี่ยนรหัสผ่านตอนล็อกอิน ======
    public void changePasswordByPrincipal(String principalKey, ChangePasswordRequest req) {
        if (!StringUtils.hasText(principalKey)) {
            throw new ApiException("UNAUTHORIZED", "Unauthorized");
        }

        // หา user: ลองตีความเป็น email ก่อน ถ้าไม่เจอค่อยลองเป็น id
        var key = principalKey.trim();
        var userOpt = userRepository.findByEmail(key.toLowerCase());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findById(key);
        }
        var user = userOpt.orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found."));

        String current = req.getCurrentPassword() == null ? "" : req.getCurrentPassword();
        String next    = req.getNewPassword() == null ? "" : req.getNewPassword();

        if (next.length() < 8) {
            throw new ApiException("WEAK_PASSWORD", "Password must be at least 8 characters.");
        }

        String stored = user.getPasswordHash(); // ใช้ฟิลด์ตาม entity ของคุณ
        if (!StringUtils.hasText(stored) || !passwordEncoder.matches(current, stored)) {
            throw new ApiException("INVALID_CREDENTIALS", "Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(next));
        userRepository.save(user);
    }
}
