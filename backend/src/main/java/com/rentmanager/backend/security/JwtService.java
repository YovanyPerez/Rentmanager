package com.rentmanager.backend.security;

import com.rentmanager.backend.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtEncoder encoder;
  private final JwtProperties properties;

  public JwtService(JwtEncoder encoder, JwtProperties properties) {
    this.encoder = encoder;
    this.properties = properties;
  }

  public IssuedToken issue(User user) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(properties.ttlMinutes(), ChronoUnit.MINUTES);
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("rentmanager")
        .subject(user.getEmail())
        .issuedAt(now)
        .expiresAt(expiresAt)
        .claim("role", user.getRole().name())
        .claim("uid", user.getId())
        .claim("name", user.getFullName())
        .build();
    Jwt jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    return new IssuedToken(jwt.getTokenValue(), expiresAt);
  }

  public record IssuedToken(String token, Instant expiresAt) {}
}
