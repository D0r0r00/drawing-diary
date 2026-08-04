package com.drawingdiary.backend.domain.user;

import com.drawingdiary.backend.domain.user.dto.UserResponse;
import com.drawingdiary.backend.domain.user.dto.UserSearchResponse;
import com.drawingdiary.backend.domain.user.dto.UserUpdateRequest;
import com.drawingdiary.backend.domain.user.dto.UserUpdateResponse;
import com.drawingdiary.backend.domain.user.exception.DuplicateNicknameException;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import com.drawingdiary.backend.security.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getProfileImageUrl());
    }

    @Transactional
    public UserUpdateResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getUserOrThrow(userId);
        if (!user.getNickname().equals(request.nickname()) && userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateNicknameException(request.nickname());
        }
        user.updateProfile(request.nickname(), request.profileImageUrl());
        return new UserUpdateResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    /**
     * Soft delete: @SQLDelete on the entity turns this into an UPDATE that sets
     * deleted_at, and @SQLRestriction hides the row from later queries. The
     * refresh token is dropped so the deleted account cannot mint new sessions.
     */
    @Transactional
    public void deleteMe(Long userId) {
        User user = getUserOrThrow(userId);
        userRepository.delete(user);
        refreshTokenStore.delete(userId);
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
