// src/main/java/com/shirtshop/service/AddressService.java
package com.shirtshop.service;

import com.shirtshop.dto.address.AddressRequest;
import com.shirtshop.dto.address.AddressResponse;
import com.shirtshop.entity.Address;
import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final UserRepository userRepo;

    private User getUserOrThrow(String userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public List<AddressResponse> list(String userId) {
        return getUserOrThrow(userId).getAddresses().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    public AddressResponse upsert(String userId, AddressRequest req) {
        validate(req);
        User u = getUserOrThrow(userId);

        Address target;
        if (StringUtils.hasText(req.getId())) {
            // update
            target = u.getAddresses().stream()
                    .filter(a -> a.getId().equals(req.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        } else {
            // create
            target = new Address();
            target.setId(UUID.randomUUID().toString());
            u.getAddresses().add(target);
        }

        target.setFullName(req.getFullName().trim());
        target.setPhone(req.getPhone().trim());
        target.setAddressLine1(req.getAddressLine1().trim());
        target.setSubdistrict(req.getSubdistrict().trim());
        target.setDistrict(req.getDistrict().trim());
        target.setProvince(req.getProvince().trim());
        target.setPostalCode(req.getPostalCode().trim());

        // จัดการ default ให้มีอันเดียว
        boolean wantDefault = Boolean.TRUE.equals(req.getIsDefault());
        if (wantDefault) {
            u.getAddresses().forEach(a -> a.setDefault(false));
            target.setDefault(true);
        } else if (u.getAddresses().stream().noneMatch(Address::isDefault)) {
            // ถ้าไม่มี default เลย → บังคับให้ตัวแรกเป็น default
            target.setDefault(true);
        }

        userRepo.save(u);
        return toResp(target);
    }

    public void delete(String userId, String addressId) {
        User u = getUserOrThrow(userId);
        boolean removed = u.getAddresses().removeIf(a -> a.getId().equals(addressId));
        if (!removed) throw new IllegalArgumentException("Address not found");

        // ถ้าลบ default ออกไป → ตั้งอันแรกเป็น default แทน
        if (u.getAddresses().stream().noneMatch(Address::isDefault) && !u.getAddresses().isEmpty()) {
            u.getAddresses().get(0).setDefault(true);
        }

        userRepo.save(u);
    }

    private void validate(AddressRequest r) {
        if (!StringUtils.hasText(r.getFullName())) throw new IllegalArgumentException("fullName required");
        if (!StringUtils.hasText(r.getPhone())) throw new IllegalArgumentException("phone required");
        if (!StringUtils.hasText(r.getAddressLine1())) throw new IllegalArgumentException("addressLine1 required");
        if (!StringUtils.hasText(r.getSubdistrict())) throw new IllegalArgumentException("subdistrict required");
        if (!StringUtils.hasText(r.getDistrict())) throw new IllegalArgumentException("district required");
        if (!StringUtils.hasText(r.getProvince())) throw new IllegalArgumentException("province required");
        if (!StringUtils.hasText(r.getPostalCode())) throw new IllegalArgumentException("postalCode required");
        if (!r.getPostalCode().matches("\\d{5}")) throw new IllegalArgumentException("postalCode invalid");
        if (!r.getPhone().matches("0\\d{8,9}")) throw new IllegalArgumentException("phone invalid");
    }

    private AddressResponse toResp(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .subdistrict(a.getSubdistrict())
                .district(a.getDistrict())
                .province(a.getProvince())
                .postalCode(a.getPostalCode())
                .isDefault(a.isDefault())
                .build();
    }
}
