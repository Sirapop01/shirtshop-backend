package com.shirtshop.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data @Builder
public class UserDetailResponse {
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;
    private String profileImageUrl;

    private Set<String> roles;
    private boolean active;
    private Instant lastActive;

    private Instant createdAt;
    private Instant updatedAt;

    // ถ้าจะโชว์ address ด้วยก็ใส่เป็น DTO ย่อยได้ เช่น List<AddressDTO>
}
