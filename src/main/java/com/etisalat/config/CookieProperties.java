package com.etisalat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cookie")
@Data
public class CookieProperties {

    private boolean secure;
    private String sameSite;
    private String domain;
    private String path;

}
