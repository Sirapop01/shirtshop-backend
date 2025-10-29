package com.shirtshop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudinaryUploadResponse {
    @JsonProperty("public_id")
    private String publicId;

    @JsonProperty("secure_url")
    private String secureUrl;  // ✅ เพิ่ม

    private String url;        // http (fallback)
}
