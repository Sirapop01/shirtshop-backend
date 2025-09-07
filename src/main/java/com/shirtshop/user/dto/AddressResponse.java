package com.shirtshop.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressResponse {
    private String id;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postcode;
    private String recipientFirstName;
    private String recipientLastName;
    private String phone;
    private boolean isDefault;
}
