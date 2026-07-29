package com.drawingdiary.backend.domain.user;

import com.drawingdiary.backend.domain.user.dto.UserResponse;
import com.drawingdiary.backend.domain.user.dto.UserSearchResponse;
import com.drawingdiary.backend.domain.user.dto.UserUpdateRequest;
import com.drawingdiary.backend.domain.user.dto.UserUpdateResponse;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl());
    }

    @Transactional
    public UserUpdateResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getUserOrThrow(userId);
        user.updateProfile(request.nickname(), request.profileImageUrl());
        return new UserUpdateResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        return userRepository.findByNicknameContainingIgnoreCase(keyword).stream()
                .map(user -> new UserSearchResponse(user.getId(), user.getNickname(), user.getProfileImageUrl()))
                .toList();
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
