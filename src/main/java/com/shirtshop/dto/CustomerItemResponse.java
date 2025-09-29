// src/main/java/com/shirtshop/dto/CustomerItemResponse.java
package com.shirtshop.dto;

import lombok.*;
import java.time.Instant;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerItemResponse {
    private String id;
    private String name;          // แสดงในคอลัมน์ NAME
    private String email;         // คอลัมน์ EMAIL
    private Set<String> roles;    // คอลัมน์ ROLES  (เช่น ["USER"], ["ADMIN"])
    private Boolean active;       // คอลัมน์ STATUS (Active/Inactive)
    private Instant lastActive;   // คอลัมน์ LAST ACTIVE (เช่น "1 Hours ago")
}
