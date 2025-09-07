package com.shirtshop.config;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secretBase64;
    private int accessTokenMinutes;
    private int refreshTokenDays;
}
