package com.drawingdiary.backend.domain.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 서버 접속 설정. 주소를 코드에 박아두면 로컬에서 띄운 AI 서버로 바꿔 붙을 수 없어
 * 설정으로 뺐다(application.yml의 ai.server.*, 환경변수 AI_SERVER_URL).
 *
 * @param url            AI 서버 베이스 URL. 끝의 /는 붙이지 않는다
 * @param connectTimeout 연결 수립까지. 서버가 죽었는지를 빨리 판단하는 값이라 짧게
 * @param readTimeout    응답 대기. Render 무료 티어는 유휴 상태에서 깨어나는 데만
 *                       수십 초가 걸려(실측 52초) 넉넉히 잡아야 한다
 */
@ConfigurationProperties(prefix = "ai.server")
public record AiServerProperties(
        String url,
        Duration connectTimeout,
        Duration readTimeout
) {
}
