package com.shirtshop.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.Set;

@Document(collection = "users")
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String username; // ไม่บังคับตอนสมัครก็ได้

    private String firstName;
    private String lastName;
    private String displayName; // จะใช้แสดงชื่อบน FE

    private String passwordHash;

    private String phone;
    private String profileImageUrl; // Cloudinary URL

    @Builder.Default
    private Set<String> roles = Set.of("USER");

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private boolean emailVerified; // เผื่ออนาคตทำ verify
}
