package com.stevenmoriasi.insurance.integrations.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, @Value("${insurance.security.client-id}") String clientId)
            throws Exception {
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleMapper(clientId));
        authenticationConverter.setPrincipalClaimName("preferred_username");

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers("/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers("/callbacks/payments/**")
                                        .permitAll()
                                        .requestMatchers("/actuator/**")
                                        .hasRole("PLATFORM_ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer.jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        authenticationConverter)));
        return http.build();
    }
}
