package com.shirtshop.user;

import com.shirtshop.security.CurrentUser;
import com.shirtshop.user.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService service;

    @GetMapping
    public ResponseEntity<?> list() {
        String userId = CurrentUser.idOrThrow();
        return ResponseEntity.ok(service.listMy(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AddressCreateRequest req) {
        String userId = CurrentUser.idOrThrow();
        return ResponseEntity.ok(service.createMy(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @Valid @RequestBody AddressUpdateRequest req) {
        String userId = CurrentUser.idOrThrow();
        return ResponseEntity.ok(service.updateMy(userId, id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        String userId = CurrentUser.idOrThrow();
        service.deleteMy(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefault(@PathVariable String id) {
        String userId = CurrentUser.idOrThrow();
        return ResponseEntity.ok(service.setDefaultMy(userId, id));
    }
}
