package com.stevenmoriasi.insurance.bff.security;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
                    String clientId)
            throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName("XSRF-TOKEN");
        csrfRepository.setHeaderName("X-XSRF-TOKEN");
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        OidcRoleMapper roleMapper = new OidcRoleMapper(clientId);
        http.authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers(
                                                "/",
                                                "/assets/**",
                                                "/favicon.ico",
                                                "/api/public/**",
                                                "/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("PLATFORM_ADMIN")
                                        .requestMatchers("/api/process/**")
                                        .hasAnyRole("PROCESS_OWNER", "PLATFORM_ADMIN")
                                        .requestMatchers("/api/cases/**")
                                        .hasAnyRole(
                                                "CLAIMS_OFFICER",
                                                "CLAIMS_ASSESSOR",
                                                "CLAIMS_APPROVER",
                                                "SENIOR_CLAIMS_APPROVER",
                                                "CLAIMS_SUPERVISOR",
                                                "FINANCE_OPERATOR",
                                                "PROCESS_OWNER",
                                                "PLATFORM_ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(csrfRepository)
                                        .csrfTokenRequestHandler(csrfHandler))
                .exceptionHandling(
                        exceptions ->
                                exceptions.defaultAuthenticationEntryPointFor(
                                        new HttpStatusEntryPoint(UNAUTHORIZED),
                                        request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(
                        oauth ->
                                oauth.userInfoEndpoint(
                                        userInfo ->
                                                userInfo.userAuthoritiesMapper(
                                                        roleMapper::mapAuthorities)))
                .oauth2Client(Customizer.withDefaults())
                .logout(
                        logout ->
                                logout.logoutUrl("/api/session/logout")
                                        .logoutSuccessUrl("/")
                                        .invalidateHttpSession(true)
                                        .clearAuthentication(true)
                                        .deleteCookies("INSURANCE_SESSION"))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                policy ->
                                                        policy.policyDirectives(
                                                                "default-src 'self'; connect-src"
                                                                    + " 'self'; img-src 'self'"
                                                                    + " data:; style-src 'self'"
                                                                    + " 'unsafe-inline'; script-src"
                                                                    + " 'self'; frame-ancestors"
                                                                    + " 'none'"))
                                        .frameOptions(frame -> frame.deny())
                                        .referrerPolicy(
                                                referrer ->
                                                        referrer.policy(
                                                                org.springframework.security.web
                                                                        .header.writers
                                                                        .ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER)));
        return http.build();
    }
}
