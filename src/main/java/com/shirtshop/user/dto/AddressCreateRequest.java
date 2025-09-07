package com.shirtshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressCreateRequest {
    @NotBlank private String addressLine1;
    private String addressLine2;
    @NotBlank private String city;
    @NotBlank private String state;
    @NotBlank private String postcode;          // แนะนำตรวจ regex ^\\d{5}$
    @NotBlank private String recipientFirstName;
    @NotBlank private String recipientLastName;
    @NotBlank private String phone;             // ใส่ regex ไทยภายหลังได้
    private boolean isDefault;
}
