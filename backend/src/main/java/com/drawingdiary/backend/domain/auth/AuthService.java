package com.drawingdiary.backend.domain.auth;

import com.drawingdiary.backend.domain.auth.dto.LoginRequest;
import com.drawingdiary.backend.domain.auth.dto.LoginResponse;
import com.drawingdiary.backend.domain.auth.dto.LogoutRequest;
import com.drawingdiary.backend.domain.auth.dto.SignupRequest;
import com.drawingdiary.backend.domain.auth.dto.SignupResponse;
import com.drawingdiary.backend.domain.auth.dto.TokenRefreshRequest;
import com.drawingdiary.backend.domain.auth.dto.TokenRefreshResponse;
import com.drawingdiary.backend.domain.auth.exception.AuthTokenException;
import com.drawingdiary.backend.domain.auth.exception.DuplicateEmailException;
import com.drawingdiary.backend.domain.auth.exception.InvalidCredentialsException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.DuplicateNicknameException;
import com.drawingdiary.backend.security.AuthErrorCode;
import com.drawingdiary.backend.security.JwtTokenProvider;
import com.drawingdiary.backend.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException(request.nickname());
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        User saved = userRepository.save(user);

        return new SignupResponse(saved.getId(), saved.getEmail(), saved.getNickname());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken, user.getId());
    }

    /**
     * accessToken 재발급. refreshToken 자체는 새로 만들지 않고 기존 것을 계속 쓴다 —
     * 재발급 때마다 refreshToken까지 갈아끼우면 응답이 유실된 클라이언트가 두 토큰을 모두
     * 잃고 로그아웃되기 때문이다.
     *
     * 서명이 유효한 것만으로는 부족해서 Redis에 저장된 값과 대조한다. 그래야 로그아웃이나
     * 탈퇴로 폐기된 토큰이 만료 전까지 계속 accessToken을 찍어내는 일이 없다.
     */
    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(refreshToken, JwtTokenProvider.TokenType.REFRESH);
        } catch (ExpiredJwtException e) {
            throw new AuthTokenException(AuthErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthTokenException(AuthErrorCode.TOKEN_INVALID);
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthTokenException(AuthErrorCode.TOKEN_INVALID);
        }

        if (!refreshToken.equals(refreshTokenStore.find(userId))) {
            throw new AuthTokenException(AuthErrorCode.TOKEN_INVALID);
        }

        // 탈퇴한 계정은 @SQLRestriction 때문에 여기서 비어 나온다. 저장소의 토큰도 탈퇴
        // 시점에 지워지므로 보통 위에서 걸리지만, 남아 있더라도 새 토큰이 나가지 않는다.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthTokenException(AuthErrorCode.TOKEN_INVALID));

        return new TokenRefreshResponse(
                jwtTokenProvider.createAccessToken(user.getId(), user.getEmail()),
                user.getId());
    }

    /**
     * Idempotent by design: a client that presents an expired, malformed, or
     * already-revoked token is still considered logged out, so this never fails
     * the request. Only a token that is currently the stored one is revoked,
     * which keeps a stale token from evicting a newer session.
     */
    public void logout(LogoutRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return;
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        if (refreshToken.equals(refreshTokenStore.find(userId))) {
            refreshTokenStore.delete(userId);
        }
    }
}
