package com.shirtshop.auth.ResetPassword;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PasswordResetRepository extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);
    void deleteByEmail(String email);
}
