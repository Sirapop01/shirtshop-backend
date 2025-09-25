package com.shirtshop.service;

import com.shirtshop.dto.address.AddressRequest;
import com.shirtshop.dto.address.AddressResponse;
import com.shirtshop.entity.Address;
import com.shirtshop.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final MongoTemplate mongoTemplate;

    /* ---------- helper: เคลียร์ default อื่นของ user ---------- */
    private void unsetOtherDefaults(String userId, String keepId) {
        Query q = new Query(Criteria.where("userId").is(userId).and("_id").ne(keepId));
        Update u = new Update().set("isDefault", false);
        mongoTemplate.updateMulti(q, u, Address.class);
    }

    public List<AddressResponse> getAllByUserId(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AddressResponse create(String userId, AddressRequest req) {
        Address a = Address.builder()
                .userId(userId)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .addressLine1(req.getAddressLine1())
                .subdistrict(req.getSubdistrict())
                .district(req.getDistrict())
                .province(req.getProvince())
                .postalCode(req.getPostalCode())
                .isDefault(req.isDefault())
                .build();

        a = addressRepository.save(a);

        if (a.isDefault()) {
            unsetOtherDefaults(userId, a.getId());
        } else {
            // ถ้ายังไม่มี default ใดเลย ให้ตัวแรกเป็น default อัตโนมัติ (optional)
            if (addressRepository.findByUserIdAndIsDefaultTrue(userId).isEmpty()) {
                a.setDefault(true);
                a = addressRepository.save(a);
                unsetOtherDefaults(userId, a.getId());
            }
        }
        return AddressResponse.fromEntity(a);
    }

    public AddressResponse update(String userId, String id, AddressRequest req) {
        Address existing = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        existing.setFullName(req.getFullName());
        existing.setPhone(req.getPhone());
        existing.setAddressLine1(req.getAddressLine1());
        existing.setSubdistrict(req.getSubdistrict());
        existing.setDistrict(req.getDistrict());
        existing.setProvince(req.getProvince());
        existing.setPostalCode(req.getPostalCode());
        existing.setDefault(req.isDefault());

        existing = addressRepository.save(existing);

        if (existing.isDefault()) {
            unsetOtherDefaults(userId, existing.getId());
        } else {
            // ป้องกันไม่ให้ user ไม่มี default เลย (optional)
            if (addressRepository.findByUserIdAndIsDefaultTrue(userId).isEmpty()) {
                existing.setDefault(true);
                existing = addressRepository.save(existing);
                unsetOtherDefaults(userId, existing.getId());
            }
        }
        return AddressResponse.fromEntity(existing);
    }

    public void delete(String userId, String id) {
        Address existing = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        boolean wasDefault = existing.isDefault();
        addressRepository.delete(existing);

        // ถ้าลบ default ตัวเดิมทิ้ง ให้ตั้งตัวใดตัวหนึ่งเป็น default อัตโนมัติ (optional)
        if (wasDefault) {
            List<Address> left = addressRepository.findByUserId(userId);
            if (!left.isEmpty()) {
                Address pick = left.get(0);
                pick.setDefault(true);
                pick = addressRepository.save(pick);
                unsetOtherDefaults(userId, pick.getId());
            }
        }
    }

    /* ---------- เมธอดลัด: set default ตาม id ---------- */
    public AddressResponse setDefault(String userId, String id) {
        Address target = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!target.isDefault()) {
            target.setDefault(true);
            target = addressRepository.save(target);
        }
        unsetOtherDefaults(userId, target.getId());
        return AddressResponse.fromEntity(target);
    }
}
