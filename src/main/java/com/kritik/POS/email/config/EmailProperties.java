package com.kritik.POS.email.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private String loginUrl = "http://localhost:3000/login";
    private String passwordResetUrl = "http://localhost:8080/reset-password";
    private String fromName = "POS Support";
}
