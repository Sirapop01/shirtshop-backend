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
import org.springframework.web.multipart.MultipartFile;
import com.shirtshop.dto.CloudinaryUploadResponse;
import com.shirtshop.dto.UpdateUserRequest; // 👈 เพิ่ม import นี้
import org.springframework.util.StringUtils; // 👈 ตรวจสอบว่ามี import นี้

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // มาจาก config bean
    private final CloudinaryService cloudinaryService;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    public UserResponse register(RegisterRequest req, MultipartFile profileImage) {
        String imagePublicId = null;
        String imageUrl = null;

        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new ApiException("EMAIL_ALREADY_USED", "Email is already registered.");
        }

        // ⭐️⭐️ [USERNAME FIX] ถ้ามีการส่ง username มา ให้เช็คซ้ำก่อน ⭐️⭐️
        if (StringUtils.hasText(req.getUsername()) &&
                userRepository.existsByUsername(req.getUsername().toLowerCase())) {
            throw new ApiException("USERNAME_ALREADY_USED", "Username is already taken.");
        }

        // อัปโหลดรูปภาพ (เหมือนเดิม)

        if (profileImage != null && !profileImage.isEmpty()) {
            // ⭐️ 1. รับค่าเป็น CloudinaryUploadResponse
            CloudinaryUploadResponse response = cloudinaryService.uploadFile(profileImage, "avatars");
            // ⭐️ 2. ดึงค่า url และ publicId จาก object response
            imageUrl = response.getUrl();
            imagePublicId = response.getPublicId();
        }

        String displayName = req.getDisplayName();
        if (!StringUtils.hasText(displayName)) {
            // ถ้าไม่ได้ส่ง displayName มา ให้สร้างจาก FirstName + LastName
            displayName = String.format("%s %s",
                    StringUtils.hasText(req.getFirstName()) ? req.getFirstName() : "",
                    StringUtils.hasText(req.getLastName()) ? req.getLastName() : ""
            ).trim();
        }


        String username = req.getUsername();
        if (!StringUtils.hasText(username)) {
            // ถ้าไม่ได้ส่ง username มา ให้สร้างจากส่วนหน้าของ email
            username = req.getEmail().split("@")[0];
        }

        // ตรวจสอบ username ที่สร้างจาก email ซ้ำอีกครั้ง
        if (userRepository.existsByUsername(username.toLowerCase())) {
            throw new ApiException("USERNAME_ALREADY_USED", "Generated username '" + username + "' is already taken.");
        }


        User user = User.builder()
                .email(req.getEmail().toLowerCase())
                .username(username.toLowerCase()) // ใช้ username ที่ผ่านการตรวจสอบแล้ว
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .displayName(displayName) // ใช้ displayName ที่สร้างขึ้น
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .profileImageUrl(imageUrl)
                .profileImagePublicId(imagePublicId)
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
                .roles(u.getRoles())
                .build();
    }

    public User updateUserProfile(String userId, UpdateUserRequest request) {
        // 1. ค้นหา User จาก ID, ถ้าไม่เจอจะโยน Exception
        User user = findByIdOrThrow(userId);

        // 2. อัปเดตค่า field ต่างๆ หากมีการส่งค่าใหม่มา
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }

        // 3. อัปเดต displayName ใหม่อัตโนมัติ ถ้ามีการเปลี่ยนชื่อ
        // ใช้ Logic เดียวกับตอนสมัครสมาชิก
        if (StringUtils.hasText(request.getFirstName()) || StringUtils.hasText(request.getLastName())) {
            String newDisplayName = String.format("%s %s", user.getFirstName(), user.getLastName()).trim();
            user.setDisplayName(newDisplayName);
        }

        // 4. บันทึกการเปลี่ยนแปลงลง Database
        return userRepository.save(user);
    }

    public Optional<User> getById(String id) {
        return userRepository.findById(id);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    // UserService.java
    public User findByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
    }

}
