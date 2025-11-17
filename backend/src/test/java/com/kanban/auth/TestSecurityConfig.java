package com.kanban.auth;

import com.kanban.auth.oauth.OAuth2AuthenticationFailureHandler;
import com.kanban.auth.oauth.OAuth2AuthenticationSuccessHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트용 Security 설정
 * OAuth2 핸들러를 Mock으로 제공
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return mock(OAuth2AuthenticationSuccessHandler.class);
    }

    @Bean
    @Primary
    public OAuth2AuthenticationFailureHandler oAuth2FailureHandler() {
        return mock(OAuth2AuthenticationFailureHandler.class);
    }
}
