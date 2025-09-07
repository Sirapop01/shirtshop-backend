package com.shirtshop.user;

import com.shirtshop.user.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserRepository userRepo;

    public List<AddressResponse> listMy(String userId) {
        var user = userRepo.findById(userId).orElseThrow();
        var list = user.getAddresses() == null ? List.<Address>of() : user.getAddresses();
        return list.stream().map(this::toResp).collect(Collectors.toList());
    }

    public AddressResponse createMy(String userId, AddressCreateRequest req) {
        var user = userRepo.findById(userId).orElseThrow();
        var list = user.getAddresses() == null ? new ArrayList<Address>() : new ArrayList<>(user.getAddresses());

        Address addr = Address.builder()
                .id(UUID.randomUUID().toString())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .state(req.getState())
                .postcode(req.getPostcode())
                .recipientFirstName(req.getRecipientFirstName())
                .recipientLastName(req.getRecipientLastName())
                .phone(req.getPhone())
                .isDefault(false)
                .build();

        // ถ้าขอ default ให้บังคับเหลือแค่ 1
        if (req.isDefault() || list.isEmpty()) {
            list.forEach(a -> a.setDefault(false));
            addr.setDefault(true);
        }
        list.add(addr);
        user.setAddresses(list);
        userRepo.save(user);
        return toResp(addr);
    }

    public AddressResponse updateMy(String userId, String addressId, AddressUpdateRequest req) {
        var user = userRepo.findById(userId).orElseThrow();
        var list = user.getAddresses();
        if (list == null) throw new NoSuchElementException("No addresses");
        var idx = findIndex(list, addressId);
        var a = list.get(idx);

        a.setAddressLine1(req.getAddressLine1());
        a.setAddressLine2(req.getAddressLine2());
        a.setCity(req.getCity());
        a.setState(req.getState());
        a.setPostcode(req.getPostcode());
        a.setRecipientFirstName(req.getRecipientFirstName());
        a.setRecipientLastName(req.getRecipientLastName());
        a.setPhone(req.getPhone());

        userRepo.save(user);
        return toResp(a);
    }

    public void deleteMy(String userId, String addressId) {
        var user = userRepo.findById(userId).orElseThrow();
        var list = user.getAddresses();
        if (list == null) return;
        boolean removedDefault = false;
        Iterator<Address> it = list.iterator();
        while (it.hasNext()) {
            Address a = it.next();
            if (a.getId().equals(addressId)) {
                removedDefault = a.isDefault();
                it.remove();
                break;
            }
        }
        // ถ้าลบ default ออก และยังมีที่อยู่เหลือ → ตั้งรายการแรกเป็น default
        if (removedDefault && !list.isEmpty()) {
            list.forEach(a -> a.setDefault(false));
            list.get(0).setDefault(true);
        }
        userRepo.save(user);
    }

    public AddressResponse setDefaultMy(String userId, String addressId) {
        var user = userRepo.findById(userId).orElseThrow();
        var list = user.getAddresses();
        if (list == null || list.isEmpty()) throw new NoSuchElementException("No addresses");
        var idx = findIndex(list, addressId);
        list.forEach(a -> a.setDefault(false));
        list.get(idx).setDefault(true);
        userRepo.save(user);
        return toResp(list.get(idx));
    }

    private int findIndex(List<Address> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getId().equals(id)) return i;
        throw new NoSuchElementException("Address not found: " + id);
    }

    private AddressResponse toResp(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .postcode(a.getPostcode())
                .recipientFirstName(a.getRecipientFirstName())
                .recipientLastName(a.getRecipientLastName())
                .phone(a.getPhone())
                .isDefault(a.isDefault())
                .build();
    }
}
