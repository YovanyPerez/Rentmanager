package com.rentmanager.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain apiSecurity(HttpSecurity http,
      JwtAuthenticationConverter jwtAuthenticationConverter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(SessionManagementConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health").permitAll()
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/users/**").hasRole("ADMIN")
            .requestMatchers("/api/owners/**", "/api/tenants/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/properties/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/properties/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/properties/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/properties/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/properties/**").hasAnyRole("ADMIN", "OWNER")
            .requestMatchers(HttpMethod.POST, "/api/contracts/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/contracts/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/contracts/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/contracts/**").hasAnyRole("ADMIN", "OWNER", "TENANT")
            .requestMatchers(HttpMethod.POST, "/api/payments/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/payments/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/payments/**").hasAnyRole("ADMIN", "OWNER", "TENANT")
            .requestMatchers(HttpMethod.POST, "/api/maintenance/**").hasAnyRole("ADMIN", "TENANT")
            .requestMatchers(HttpMethod.PUT, "/api/maintenance/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/maintenance/**").hasAnyRole("ADMIN", "OWNER", "TENANT")
            .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler));
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(hmacKey(properties)));
  }

  @Bean
  public JwtDecoder jwtDecoder(JwtProperties properties) {
    return NimbusJwtDecoder.withSecretKey(hmacKey(properties)).macAlgorithm(MacAlgorithm.HS256).build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("role");
    authorities.setAuthorityPrefix("ROLE_");
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    return converter;
  }

  private static SecretKeySpec hmacKey(JwtProperties properties) {
    return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }
}
