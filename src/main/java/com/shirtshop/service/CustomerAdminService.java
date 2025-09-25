package com.shirtshop.service;

import com.shirtshop.dto.CustomerItemResponse;
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

    /** Map User → CustomerItemResponse */
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
                .active(Boolean.TRUE.equals(u.isActive()))   // ✅ ใช้ isActive() แทน
                .lastActive(u.getLastActive())
                .build();
    }
}
