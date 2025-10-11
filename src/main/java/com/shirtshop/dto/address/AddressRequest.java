// src/main/java/com/shirtshop/dto/address/AddressRequest.java
package com.shirtshop.dto.address;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressRequest {
    private String id;            // null = create, not null = update
    private String fullName;
    private String phone;
    private String addressLine1;
    private String subdistrict;
    private String district;
    private String province;
    private String postalCode;
    private Boolean isDefault;    // optional
}
