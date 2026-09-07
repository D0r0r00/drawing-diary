package com.drawingdiary.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * 실패 사유를 {@link JwtAuthenticationEntryPoint}로 넘기는 통로.
     *
     * 필터에서 예외를 던지지 않고 요청 속성에 남기기만 하는 이유는, 토큰이 잘못됐더라도
     * 그 경로가 permitAll일 수 있기 때문이다(예: 만료된 토큰을 헤더에 단 채 /api/auth/login
     * 호출). 인증이 실제로 필요한지는 뒤의 인가 단계가 판단하고, EntryPoint는 그때만 불린다.
     */
    private static final String ERROR_CODE_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".errorCode";

    private final JwtTokenProvider jwtTokenProvider;

    static AuthErrorCode resolveErrorCode(HttpServletRequest request) {
        Object attribute = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        // 속성이 없으면 필터가 검사할 토큰 자체를 받지 못한 것이다.
        return attribute instanceof AuthErrorCode errorCode ? errorCode : AuthErrorCode.TOKEN_MISSING;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token, JwtTokenProvider.TokenType.ACCESS);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        Long.valueOf(claims.getSubject()), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                // 유일하게 refresh로 회복 가능한 실패라서 따로 구분한다.
                request.setAttribute(ERROR_CODE_ATTRIBUTE, AuthErrorCode.TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                // 서명 불일치, 형식 오류, 토큰 타입 오용, sub가 숫자가 아닌 경우까지.
                request.setAttribute(ERROR_CODE_ATTRIBUTE, AuthErrorCode.TOKEN_INVALID);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
