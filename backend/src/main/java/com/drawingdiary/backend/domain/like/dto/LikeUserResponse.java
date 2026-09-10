package com.drawingdiary.backend.domain.like.dto;

/**
 * 좋아요를 누른 사람. 팔로워 목록(FollowUserResponse)과 같은 필드 구성이라 프론트가
 * 같은 아바타 리스트 컴포넌트를 재사용할 수 있다.
 */
public record LikeUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
