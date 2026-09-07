package com.drawingdiary.backend.domain.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiServerProperties.class)
public class AiServerConfig {

    /**
     * AI 서버 전용 RestClient. 기본 RestClient.Builder는 타임아웃이 무제한이라, AI 서버가
     * 응답하지 않으면 요청 스레드가 영원히 잡힌다. 전용 빈으로 분리한 이유는 이 타임아웃이
     * 다른 외부 호출(완성 이미지 다운로드 등)에까지 적용되면 곤란하기 때문.
     *
     * WebClient가 아니라 RestClient인 이유: 이 프로젝트는 spring-boot-starter-web(서블릿)만
     * 쓰고 WebFlux 의존성이 없다. WebClient를 쓰려면 리액터 스택을 통째로 들여야 하는데,
     * 병렬 호출은 가상 스레드로 충분해서 의존성을 늘릴 이유가 없다.
     */
    @Bean
    public RestClient aiServerRestClient(AiServerProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(factory)
                .build();
    }
}
