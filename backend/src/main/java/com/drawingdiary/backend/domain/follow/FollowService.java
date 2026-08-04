package com.drawingdiary.backend.domain.follow;

import com.drawingdiary.backend.domain.follow.dto.FollowUserResponse;
import com.drawingdiary.backend.domain.follow.exception.AlreadyFollowingException;
import com.drawingdiary.backend.domain.follow.exception.SelfFollowException;
import com.drawingdiary.backend.domain.user.User;
import com.drawingdiary.backend.domain.user.UserRepository;
import com.drawingdiary.backend.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * The self-follow guard runs before any lookup so the CHECK constraint on
     * follows is never reached: a constraint violation would surface as a 500,
     * and the caller deserves a 400.
     */
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new SelfFollowException();
        }

        User follower = getUserOrThrow(followerId);
        User following = getUserOrThrow(followingId);

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new AlreadyFollowingException(followingId);
        }

        try {
            // Flushed here rather than at commit so that two concurrent follows
            // (a double-tapped button) resolve to 409 via the unique constraint
            // instead of escaping this method as a 500.
            followRepository.saveAndFlush(Follow.builder()
                    .follower(follower)
                    .following(following)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyFollowingException(followingId);
        }
    }

    /**
     * Idempotent: unfollowing someone you do not follow — or an account that no
     * longer exists — leaves the caller in the state they asked for, so it
     * succeeds rather than reporting an error the client cannot act on.
     */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> findFollowers(Long userId) {
        requireUserExists(userId);
        return toResponses(followRepository.findFollowersByFollowingId(userId));
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> findFollowings(Long userId) {
        requireUserExists(userId);
        return toResponses(followRepository.findFollowingsByFollowerId(userId));
    }

    private List<FollowUserResponse> toResponses(List<User> users) {
        return users.stream()
                .map(user -> new FollowUserResponse(user.getId(), user.getNickname(), user.getProfileImageUrl()))
                .toList();
    }

    private void requireUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
