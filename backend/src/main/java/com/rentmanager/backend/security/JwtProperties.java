package com.rentmanager.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.jwt")
public record JwtProperties(String secret, long ttlMinutes) {}
