// src/main/java/com/shirtshop/controller/CustomerAdminController.java
package com.shirtshop.controller;

import com.shirtshop.dto.CustomerItemResponse;
import com.shirtshop.service.CustomerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
@CrossOrigin
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

    @GetMapping
    public ResponseEntity<List<CustomerItemResponse>> listAllCustomers() {
        return ResponseEntity.ok(customerAdminService.getAllCustomers()); // ✅ เปลี่ยนเป็น getAllCustomers()
    }
}
