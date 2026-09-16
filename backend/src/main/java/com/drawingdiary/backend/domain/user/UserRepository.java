package com.drawingdiary.backend.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 로그인용. 인자는 이미 소문자로 정규화된 값이어야 한다(LoginIdPolicy.normalize).
     * 탈퇴한 계정은 @SQLRestriction 때문에 찾아지지 않는다.
     */
    Optional<User> findByLoginId(String loginId);

    /**
     * 가입 중복 검사용. 파생 쿼리(existsByLoginId)를 쓰지 않는 이유: @SQLRestriction이 붙어
     * 탈퇴한 계정을 못 보는데, DB의 UNIQUE 제약은 탈퇴 계정의 아이디도 막는다. 그러면 탈퇴자의
     * 아이디로 가입할 때 검사는 통과하고 저장에서 제약 위반이 나 400이 아니라 500이 된다.
     * 네이티브 쿼리는 @SQLRestriction을 타지 않으므로 탈퇴 계정까지 본다.
     */
    @Query(value = "select exists (select 1 from users where login_id = :loginId)", nativeQuery = true)
    boolean existsByLoginIdIncludingWithdrawn(@Param("loginId") String loginId);

    boolean existsByNickname(String nickname);

    List<User> findByNicknameContainingIgnoreCase(String keyword);
}
