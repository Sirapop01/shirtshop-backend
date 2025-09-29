package com.shirtshop.controller;

import com.shirtshop.dto.CustomerItemResponse;
import com.shirtshop.dto.UserResponse;
import com.shirtshop.service.CustomerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

    /** List ทั้งหมด */
    @GetMapping
    public ResponseEntity<List<CustomerItemResponse>> listAll() {
        return ResponseEntity.ok(customerAdminService.getAllCustomers());
    }

    /** ดู Profile รายบุคคล */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerAdminService.getCustomerById(id));
    }

    /** ลบลูกค้า */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        customerAdminService.deleteCustomerById(id);
        return ResponseEntity.noContent().build();
    }
}
