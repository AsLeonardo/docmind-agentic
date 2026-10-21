package dev.asleonardo.docmind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "docmind.security")
public record SecurityProperties(String jwtSecret, Duration tokenTtl) {
}
