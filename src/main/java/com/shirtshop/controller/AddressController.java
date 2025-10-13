package com.shirtshop.controller;

import com.shirtshop.dto.address.AddressRequest;
import com.shirtshop.dto.address.AddressResponse;
import com.shirtshop.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal(expression = "id") String userId) {
        return addressService.getAllByUserId(userId);
    }

    @PostMapping
    public AddressResponse create(@AuthenticationPrincipal(expression = "id") String userId,
                                  @RequestBody @Valid AddressRequest req) {
        return addressService.create(userId, req);
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal(expression = "id") String userId,
                                  @PathVariable String id,
                                  @RequestBody @Valid AddressRequest req) {
        return addressService.update(userId, id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal(expression = "id") String userId,
                       @PathVariable String id) {
        addressService.delete(userId, id);
    }

    // ✅ ตั้งค่า default โดยตรง
    @PutMapping("/{id}/default")
    public AddressResponse setDefault(@AuthenticationPrincipal(expression = "id") String userId,
                                      @PathVariable String id) {
        return addressService.setDefault(userId, id);
    }
}
