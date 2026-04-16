package com.example.ccc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String userTokenName = "Authorization";
    private String secret;
    private long accessTokenExpire = 1800;
    private long refreshTokenExpire = 604800;
}
