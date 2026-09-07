package com.drawingdiary.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/** 에는 refresh도 포함된다. 재발급은 accessToken이
                        // 이미 만료된 상태에서 호출하는 경로라 인증을 요구할 수 없다.
                        .requestMatchers("/api/auth/**").permitAll()
                        // 스프링이 에러 응답을 만들 때 /error로 다시 디스패치하는데, 이 내부
                        // 디스패치는 인증 정보를 들고 오지 않는다. /error를 막아두면 원래
                        // 상태 코드가 무엇이었든 그 재디스패치가 먼저 거부당해서, 잘못된 JSON
                        // 같은 400이 클라이언트에는 403으로 나가버린다.
                        .requestMatchers("/error").permitAll()
                        // 이미지 서빙만 공개. <img src>는 Authorization 헤더를 붙일 수 없어
                        // 인증을 요구하면 이미지가 아예 렌더링되지 않는다. 업로드(POST)는
                        // 메서드를 GET으로 한정해 그대로 인증 대상으로 남는다.
                        .requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()
                        .anyRequest().authenticated()
                )
                // 기본 EntryPoint는 403에 스프링 부트 기본 에러 본문을 내려서 실패 사유가
                // 드러나지 않는다. 401 + {code, message}로 바꿔 프론트가 만료를 식별하게 한다.
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 개발 단계 설정: 프론트 도메인이 확정되지 않아 모든 origin을 허용한다.
     * 배포 전에는 반드시 실제 도메인 목록으로 좁혀야 한다.
     *
     * allowedOrigins("*")가 아니라 allowedOriginPatterns("*")를 쓰는 이유는,
     * allowCredentials(true)와 와일드카드 "*"를 함께 두면 Spring이
     * IllegalArgumentException을 던지기 때문이다. 패턴 방식은 응답의
     * Access-Control-Allow-Origin에 "*" 대신 요청의 실제 Origin을 되돌려주므로
     * 두 설정이 양립한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        // 프리플라이트 결과를 1시간 캐시해 매 요청마다 OPTIONS가 붙는 것을 줄인다.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
