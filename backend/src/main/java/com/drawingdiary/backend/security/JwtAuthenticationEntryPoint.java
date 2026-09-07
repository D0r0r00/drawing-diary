package com.drawingdiary.backend.security;

import com.drawingdiary.backend.common.exception.AuthErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증 없이 보호된 자원에 접근했을 때의 응답을 직접 만든다.
 *
 * 이 클래스가 없으면 스프링 시큐리티 기본 동작으로 403이 나가고, 본문은 스프링 부트의
 * /error 형식({@code timestamp, status, error, path})이 된다. 그 형식으로는 만료와
 * 그 외 실패가 구분되지 않아 프론트가 자동 재발급을 걸 수 없었다.
 *
 * 실패 사유는 {@link JwtAuthenticationFilter}가 요청 속성에 남겨둔 것을 읽는다.
 * 속성이 없다는 것은 필터가 토큰을 아예 보지 못했다는 뜻이므로 TOKEN_MISSING으로 본다.
 * 여기서 응답을 직접 써버리므로 /error로의 재디스패치도 일어나지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        AuthErrorCode errorCode = JwtAuthenticationFilter.resolveErrorCode(request);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 메시지가 한글이라 charset을 명시하지 않으면 클라이언트가 깨진 문자열을 받는다.
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), AuthErrorResponse.of(errorCode));
    }
}
