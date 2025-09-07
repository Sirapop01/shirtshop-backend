package com.shirtshop.auth.ResetPassword;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final PasswordResetRepository repo;

    public String createOtpForEmail(String email) {
        // สุ่ม 6 หลัก
        String otp = String.format("%06d", new Random().nextInt(999999));
        repo.deleteByEmail(email); // เคลียร์ token เก่า

        PasswordResetToken token = PasswordResetToken.builder()
                .email(email.toLowerCase())
                .otp(otp)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
        repo.save(token);

        return otp;
    }

    public boolean validateOtp(String email, String otp) {
        var tokenOpt = repo.findByEmailAndOtp(email.toLowerCase(), otp);
        if (tokenOpt.isEmpty()) return false;
        var token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(Instant.now())) {
            repo.delete(token);
            return false;
        }
        return true;
    }
}
