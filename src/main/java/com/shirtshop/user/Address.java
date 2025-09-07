package com.shirtshop.user;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    private String id;                 // UUID
    private String addressLine1;
    private String addressLine2;
    private String city;               // เขต/อำเภอ
    private String state;              // จังหวัด
    private String postcode;           // 5 หลัก
    private String recipientFirstName;
    private String recipientLastName;
    private String phone;
    private boolean isDefault;
}
