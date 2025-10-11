// src/main/java/com/shirtshop/controller/AddressController.java
package com.shirtshop.controller;

import com.shirtshop.dto.address.AddressRequest;
import com.shirtshop.dto.address.AddressResponse;
import com.shirtshop.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService service;

    private String userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName(); // ตรวจสอบอีกทีว่าเป็น userId จริง ๆ หรือ email
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list() {
        return ResponseEntity.ok(service.list(userId()));
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody AddressRequest req) {
        req.setId(null); // บังคับให้เป็น create
        AddressResponse resp = service.upsert(userId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            @PathVariable String id,
            @Valid @RequestBody AddressRequest req) {
        req.setId(id); // บังคับเป็น update
        return ResponseEntity.ok(service.upsert(userId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(userId(), id);
        return ResponseEntity.noContent().build();
    }
}
