package com.shirtshop.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudinaryUploadResponse {
    private String publicId;
    private String url;
}