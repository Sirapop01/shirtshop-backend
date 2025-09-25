package com.shirtshop.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageInfo {
    private String publicId;
    private String url;
}
