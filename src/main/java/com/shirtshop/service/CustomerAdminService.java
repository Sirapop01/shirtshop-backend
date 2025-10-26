package com.shirtshop.service;

import com.shirtshop.dto.CustomerItemResponse;
import com.shirtshop.dto.UserDetailResponse;
import com.shirtshop.dto.UserResponse;
import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAdminService {

    private final UserRepository userRepository;

    /** ✅ สำหรับ Dashboard: รวมจำนวนลูกค้าทั้งหมด */
    public long countAll() {
        return userRepository.count();
    }

    /** ✅ สำหรับ Dashboard: จำนวนลูกค้าที่ active (ถ้ามีฟิลด์ active) */
    public long countActive() {
        // ถ้าใน UserRepository มีเมธอดนี้อยู่แล้ว ใช้งานบรรทัดล่างได้เลย:
        // return userRepository.countByActiveTrue();

        // Fallback (ถ้ายังไม่มีเมธอดบน repository):
        return userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.isActive()))
                .count();
    }

    /** ดึงลูกค้าทั้งหมด (สำหรับตารางรายชื่อ) */
    public List<CustomerItemResponse> getAllCustomers() {
        return userRepository.findAll()
                .stream()
                .map(this::toCustomerItem)
                .collect(Collectors.toList());
    }

    /** ดู Profile รายบุคคลแบบย่อ (ถ้าหน้าอื่นต้องการ) */
    public UserResponse getCustomerById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return toUserResponse(user);
    }

    /** ดูรายละเอียดโปรไฟล์รายบุคคล (สำหรับหน้า /admin/customers/[id]) */
    public UserDetailResponse getDetailById(String id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        return UserDetailResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .username(u.getUsername())
                .displayName(u.getDisplayName())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .phone(u.getPhone())
                .profileImageUrl(u.getProfileImageUrl())
                .roles(toRoleSet(u))                // Set<String>
                .active(Boolean.TRUE.equals(u.isActive()))
                .lastActive(u.getLastActive())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    /** ลบลูกค้า */
    public void deleteCustomerById(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found");
        }
        userRepository.deleteById(id);
    }

    /* -------------------- Mapper helpers -------------------- */

    /** Map User → CustomerItemResponse (สำหรับ list) */
    private CustomerItemResponse toCustomerItem(User u) {
        String name = u.getDisplayName();
        if (!StringUtils.hasText(name)) {
            String fn = StringUtils.hasText(u.getFirstName()) ? u.getFirstName().trim() : "";
            String ln = StringUtils.hasText(u.getLastName()) ? u.getLastName().trim() : "";
            name = (fn + " " + ln).trim();
            if (!StringUtils.hasText(name)) {
                name = u.getUsername();
            }
        }

        return CustomerItemResponse.builder()
                .id(u.getId())
                .name(name)
                .email(u.getEmail())
                .roles(toRoleSet(u))                // Set<String>
                .active(Boolean.TRUE.equals(u.isActive()))
                .lastActive(u.getLastActive())
                .build();
    }

    /** Map User → UserResponse (แบบย่อ) */
    private UserResponse toUserResponse(User u) {
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
                .roles(toRoleSet(u))                // Set<String>
                .active(Boolean.TRUE.equals(u.isActive()))
                .lastActive(u.getLastActive())
                .build();
    }

    /** แปลง role ของ User ให้เป็น Set<String> */
    private Set<String> toRoleSet(User u) {
        if (u.getRoles() == null) return Set.of();
        return new LinkedHashSet<>(u.getRoles());
    }
}
