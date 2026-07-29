package com.drawingdiary.backend.domain.auth;

import com.drawingdiary.backend.domain.auth.dto.LoginRequest;
import com.drawingdiary.backend.domain.auth.dto.LoginResponse;
import com.drawingdiary.backend.domain.auth.dto.SignupRequest;
import com.drawingdiary.backend.domain.auth.dto.SignupResponse;
import com.drawingdiary.backend.domain.auth.exception.DuplicateEmailException;
import com.drawingdiary.backend.domain.auth.exception.InvalidCredentialsException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
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

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getEmail());

        return new LoginResponse(accessToken, user.getId());
    }
}
