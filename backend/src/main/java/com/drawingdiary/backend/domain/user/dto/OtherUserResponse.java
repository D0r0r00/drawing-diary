package com.drawingdiary.backend.domain.user.dto;

/**
 * 남의 프로필 화면. 내 정보(UserResponse)와 달리 <b>email이 없다</b> — 남의 이메일은
 * 화면에 쓸 데가 없고, 한 번 내려주면 어느 계정이 어떤 주소를 쓰는지 누구나 수집할 수 있다.
 *
 * @param isFollowing 요청자가 이 사람을 팔로우 중인지. 프론트의 팔로우 버튼이 "팔로우"와
 *                    "팔로잉 중" 중 무엇을 그릴지 정하는 값이라, 매번 세지 않고 이 응답에
 *                    담아 화면 진입 시 추가 요청이 붙지 않게 한다. 자기 자신이면 항상 false다
 *                    (자기 팔로우는 애초에 불가능하고, 버튼도 뜨지 않는다).
 */
public record OtherUserResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String bio,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {
}
