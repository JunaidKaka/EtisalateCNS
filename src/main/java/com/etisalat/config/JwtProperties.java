package com.etisalat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {

    private String secret;
    private long accessTokenValidityMs;
    private long refreshTokenValidityMs;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }

    public void setAccessTokenValidityMs(long accessTokenValidityMs) {
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public long getRefreshTokenValidityMs() {
        return refreshTokenValidityMs;
    }

    public void setRefreshTokenValidityMs(long refreshTokenValidityMs) {
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }
}
