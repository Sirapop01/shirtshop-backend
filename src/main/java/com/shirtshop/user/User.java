package com.shirtshop.user;

import com.shirtshop.user.Address;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Document("users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String displayName;
    private String firstName;
    private String lastName;
    private String phone;

    private String passwordHash;      // null ได้ถ้าเป็น OAuth
    private Set<Role> roles;

    private AuthProvider provider;    // LOCAL/GOOGLE/FACEBOOK
    private String providerId;

    private String avatarUrl;
    private String avatarPublicId;

    private boolean enabled;

    // ✅ ใส่กลับมา และให้ builder มีค่า default เป็น list ว่าง
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
