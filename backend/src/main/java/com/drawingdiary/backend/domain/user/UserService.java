package com.drawingdiary.backend.domain.user;

import com.drawingdiary.backend.domain.follow.FollowRepository;
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
    private final FollowRepository followRepository;
    private final RefreshTokenStore refreshTokenStore;

    /**
     * 팔로워/팔로잉 수는 저장해두지 않고 매번 센다. 카운터 컬럼을 두면 팔로우/언팔로우와
     * 탈퇴까지 모두 같이 갱신해야 해서 어긋나기 쉬운데, 여기서는 대상이 한 명이라
     * 집계 쿼리 두 번으로 끝난다(사용자 조회까지 합쳐 요청당 쿼리 3번 고정, N+1 없음).
     */
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getUserOrThrow(userId);
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getBio(),
                followRepository.countFollowersByFollowingId(userId),
                followRepository.countFollowingsByFollowerId(userId)
        );
    }

    /**
     * 부분 수정: 보낸 필드만 반영하고 나머지는 그대로 둔다. 닉네임 중복 검사도
     * 닉네임을 실제로 보냈을 때만 하는데, 생략했다면 값이 바뀌지 않으므로 자기 자신의
     * 닉네임과 충돌한다고 볼 이유가 없기 때문이다.
     */
    @Transactional
    public UserUpdateResponse updateMe(Long userId, UserUpdateRequest request) {
        User user = getUserOrThrow(userId);

        String nickname = request.nickname();
        if (nickname != null
                && !user.getNickname().equals(nickname)
                && userRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException(nickname);
        }

        user.applyProfileUpdate(nickname, request.profileImageUrl(), request.bio());
        return new UserUpdateResponse(user.getId(), user.getNickname(), user.getProfileImageUrl(), user.getBio());
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
