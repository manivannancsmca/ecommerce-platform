// src/main/java/com/ecommerce/user/config/PasswordEncoderConfig.java
package com.ecommerce.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides BCrypt encoding WITHOUT enabling the Spring Security filter chain.
 * spring-security-crypto is a standalone library — no @EnableWebSecurity,
 * no SecurityFilterChain, no authentication/authorization is activated.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}