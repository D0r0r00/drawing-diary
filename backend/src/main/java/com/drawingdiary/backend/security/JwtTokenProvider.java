package com.drawingdiary.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String createAccessToken(Long userId, String email) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * Access and refresh tokens are signed with the same key, so the type claim
     * is what stops a long-lived refresh token from being accepted as a bearer
     * token on authenticated endpoints.
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE);
    }

    /**
     * 검증에 실패한 <em>이유</em>가 필요한 호출자를 위한 버전. boolean을 돌려주는 위
     * 메서드들과 달리 jjwt 예외를 그대로 흘려보내므로, 만료({@link ExpiredJwtException})와
     * 그 외 무효를 구분할 수 있다.
     *
     * 타입이 어긋난 토큰(예: refreshToken을 access 자리에 사용)은 서명 자체는 멀쩡하므로
     * jjwt가 아무 예외도 던지지 않는다. 그래서 여기서 직접 확인해 무효로 처리한다.
     *
     * @throws ExpiredJwtException 서명은 올바르나 exp가 지난 경우
     * @throws JwtException        서명 불일치, 형식 오류, 토큰 타입 불일치
     */
    public Claims parseClaims(String token, TokenType expectedType) {
        Claims claims = parseClaims(token);
        String actualType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.claimValue().equals(actualType)) {
            throw new UnsupportedJwtException(
                    "기대한 토큰 타입은 %s 인데 실제로는 %s 이다".formatted(expectedType.claimValue(), actualType));
        }
        return claims;
    }

    public enum TokenType {

        ACCESS(ACCESS_TOKEN_TYPE),
        REFRESH(REFRESH_TOKEN_TYPE);

        private final String claimValue;

        TokenType(String claimValue) {
            this.claimValue = claimValue;
        }

        public String claimValue() {
            return claimValue;
        }
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private boolean validateToken(String token, String expectedType) {
        try {
            return expectedType.equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
