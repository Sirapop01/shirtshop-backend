package com.shirtshop.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String phone;
    private String profileImageUrl;
    private boolean emailVerified;
}
