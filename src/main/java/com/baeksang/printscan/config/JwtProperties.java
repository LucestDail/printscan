package com.baeksang.printscan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey = "baeksangDefaultSecretKey123456789012345678901234567890";
    private long accessTokenValidityInSeconds = 3600; // 1시간
    private long refreshTokenValidityInSeconds = 2592000; // 30일
} 