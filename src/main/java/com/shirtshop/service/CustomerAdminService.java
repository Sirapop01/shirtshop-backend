package com.shirtshop.service;

import com.shirtshop.dto.CustomerItemResponse;
import com.shirtshop.dto.UserResponse;
import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAdminService {

    private final UserRepository userRepository;

    /** ดึงลูกค้าทั้งหมด */
    public List<CustomerItemResponse> getAllCustomers() {
        return userRepository.findAll()
                .stream()
                .map(this::toCustomerItem)
                .collect(Collectors.toList());
    }

    /** ดู Profile รายบุคคล */
    public UserResponse getCustomerById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return toUserResponse(user);
    }

    /** ลบลูกค้า */
    public void deleteCustomerById(String id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found");
        }
        userRepository.deleteById(id);
    }

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
                .roles(u.getRoles())
                .active(Boolean.TRUE.equals(u.isActive()))
                .lastActive(u.getLastActive())
                .build();
    }

    /** Map User → UserResponse (สำหรับ profile view) */
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
                .roles(u.getRoles())
                .active(u.isActive())
                .lastActive(u.getLastActive())
                .build();
    }
}
