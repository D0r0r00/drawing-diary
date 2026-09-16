package com.drawingdiary.backend.domain.auth;

import com.drawingdiary.backend.domain.auth.dto.LoginRequest;
import com.drawingdiary.backend.domain.auth.dto.LoginResponse;
import com.drawingdiary.backend.domain.auth.dto.LogoutRequest;
import com.drawingdiary.backend.domain.auth.dto.SignupRequest;
import com.drawingdiary.backend.domain.auth.dto.SignupResponse;
import com.drawingdiary.backend.domain.auth.dto.TokenRefreshRequest;
import com.drawingdiary.backend.domain.auth.dto.TokenRefreshResponse;
import com.drawingdiary.backend.domain.auth.exception.AuthTokenException;
import com.drawingdiary.backend.domain.auth.exception.DuplicateLoginIdException;
import com.drawingdiary.backend.domain.auth.exception.InvalidCredentialsException;
import com.drawingdiary.backend.domain.auth.validation.LoginIdPolicy;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * 아이디는 소문자로 바꿔 저장한다(LoginIdPolicy). "Test"로 가입하면 "test"로 저장되고,
     * 이미 "test"가 있으면 409다.
     *
     * <h4>중복 검사를 통과해도 저장에서 걸릴 수 있다</h4>
     * 같은 아이디로 두 요청이 동시에 들어오면 둘 다 검사를 통과한다. 그때 늦은 쪽은 DB의 UNIQUE
     * 제약에 걸리는데, 그대로 두면 500이 나가므로 제약 위반을 어느 컬럼이 냈는지 보고 409로
     * 바꾼다. 닉네임도 같은 처리를 받는다 — existsByNickname은 탈퇴 계정을 못 봐서, 탈퇴자의
     * 닉네임으로 가입하면 검사는 통과하고 저장에서 걸린다.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String loginId = LoginIdPolicy.normalize(request.loginId());

        if (userRepository.existsByLoginIdIncludingWithdrawn(loginId)) {
            throw new DuplicateLoginIdException(loginId);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException(request.nickname());
        }

        User user = User.builder()
                .loginId(loginId)
                .email(blankToNull(request.email()))
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicate(e, loginId, request.nickname());
        }

        return new SignupResponse(saved.getId(), saved.getLoginId(), saved.getNickname());
    }

    /**
     * 대소문자를 무시한다 — 입력을 소문자로 바꿔 찾는다. 없는 아이디와 틀린 비밀번호는
     * 같은 401로 응답해 어떤 아이디가 존재하는지 드러내지 않는다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(LoginIdPolicy.normalize(request.loginId()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getLoginId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken, user.getId());
    }

    /**
     * PostgreSQL의 제약 위반 메시지에는 "Key (login_id)=(test) already exists." 처럼 컬럼 이름이
     * 들어 있다. 제약 이름(uk_... 해시)은 환경마다 다를 수 있어 컬럼 이름으로 가른다.
     */
    private RuntimeException translateDuplicate(DataIntegrityViolationException e, String loginId, String nickname) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        if (detail.contains("(login_id)")) {
            return new DuplicateLoginIdException(loginId);
        }
        if (detail.contains("(nickname)")) {
            return new DuplicateNicknameException(nickname);
        }
        return e;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
                jwtTokenProvider.createAccessToken(user.getId(), user.getLoginId(), user.getEmail()),
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
