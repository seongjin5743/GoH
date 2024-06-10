package com.example.farm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;

@Configuration
public class SecurityConfig {
    @Value("${aes.secret.key}")
    private String encodedKey;

    @Bean
    public SecretKey secretKey() {
        return AESUtil.decodeKey(encodedKey);
    }

    @Bean
    public AESUtil aesUtil() {
        return new AESUtil();
    }
}