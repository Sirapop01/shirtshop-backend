package com.shirtshop.user;

import com.shirtshop.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public User registerLocal(RegisterRequest req) {
        var user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .displayName((req.getFirstName() + " " + req.getLastName()).trim())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .avatarUrl(req.getAvatarUrl())
                .avatarPublicId(req.getAvatarPublicId())
                .roles(Set.of(Role.USER))
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                // ✅ ไม่จำเป็นถ้ามี @Builder.Default แต่ใส่ได้ ไม่ผิด
                .addresses(new ArrayList<>())
                .build();

        return userRepo.save(user);
    }




    public User validateLogin(String email, String rawPassword) {
        var user = userRepo.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (!user.isEnabled()) throw new IllegalStateException("User disabled");
        return user;
    }

    public User getByIdOrThrow(String id) {
        return userRepo.findById(id).orElseThrow();
    }

    public boolean existsByEmail(String email) {
        return userRepo.existsByEmail(email.toLowerCase());
    }
    public void updatePassword(String email, String rawPassword) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepo.save(user);
    }



}
