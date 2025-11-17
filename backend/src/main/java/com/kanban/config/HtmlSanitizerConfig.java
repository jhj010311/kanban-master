package com.kanban.config;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTML Sanitization 설정
 * XSS 공격 방지를 위한 OWASP HTML Sanitizer 정책 정의
 */
@Configuration
public class HtmlSanitizerConfig {

    @Bean
    public PolicyFactory htmlSanitizerPolicy() {
        return new HtmlPolicyBuilder()
                // 기본 텍스트 서식
                .allowElements("p", "br", "strong", "em", "u", "strike", "s")
                // 제목
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                // 목록
                .allowElements("ul", "ol", "li")
                // 링크 (https, http 프로토콜만 허용)
                .allowElements("a")
                .allowAttributes("href").onElements("a")
                .allowAttributes("target").onElements("a")
                .allowAttributes("rel").onElements("a")
                .allowStandardUrlProtocols()
                .requireRelNofollowOnLinks()
                // 인용
                .allowElements("blockquote")
                // 코드
                .allowElements("code", "pre")
                .toFactory();
    }
}
