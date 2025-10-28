package com.shirtshop.ml.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MLGatewayError {
    private String error;
    private Integer status;
    private String details;
}
