package com.kanban.auth.security;

import com.kanban.auth.oauth2.CustomOAuth2UserService;
import com.kanban.auth.oauth2.OAuth2AuthenticationFailureHandler;
import com.kanban.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * Spec § 6. 백엔드 규격 - SecurityConfig
 * FR-06a: Google OAuth2 로그인
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // OAuth2 설정 디버깅
        System.out.println("=== OAuth2 Configuration Debug ===");
        System.out.println("ClientRegistrationRepository: " + (clientRegistrationRepository != null ? "FOUND" : "NULL"));
        System.out.println("CustomOAuth2UserService: " + (customOAuth2UserService != null ? "FOUND" : "NULL"));
        System.out.println("OAuth2AuthenticationSuccessHandler: " + (oAuth2AuthenticationSuccessHandler != null ? "FOUND" : "NULL"));
        System.out.println("OAuth2AuthenticationFailureHandler: " + (oAuth2AuthenticationFailureHandler != null ? "FOUND" : "NULL"));

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/health/**",
                                "/h2-console/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/uploads/**",  // 업로드된 파일(프로필 사진 등)은 인증 없이 접근 가능
                                "/login/oauth2/**",  // OAuth2 로그인 엔드포인트
                                "/oauth2/**"  // OAuth2 콜백 엔드포인트
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .anyRequest().authenticated()
                );

        // OAuth2 설정이 있을 때만 활성화
        if (clientRegistrationRepository != null
                && customOAuth2UserService != null
                && oAuth2AuthenticationSuccessHandler != null
                && oAuth2AuthenticationFailureHandler != null) {
            System.out.println("✅ OAuth2 Login ENABLED");
            // Spec § 6. 백엔드 규격 - SecurityConfig OAuth2 로그인 활성화
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        } else {
            System.out.println("❌ OAuth2 Login DISABLED - Missing required beans");
        }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
