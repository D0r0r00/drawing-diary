package com.drawingdiary.backend.domain.follow;

import com.drawingdiary.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    /**
     * Selecting the User entity rather than the Follow row lets @SQLRestriction
     * on User filter out soft-deleted accounts, so a withdrawn user disappears
     * from everyone's follower/following lists without needing to delete rows.
     */
    @Query("select f.follower from Follow f where f.following.id = :userId order by f.id desc")
    List<User> findFollowersByFollowingId(@Param("userId") Long userId);

    @Query("select f.following from Follow f where f.follower.id = :userId order by f.id desc")
    List<User> findFollowingsByFollowerId(@Param("userId") Long userId);

    /**
     * follows 행을 그냥 세지 않고 User로 조인해서 세는 이유는 위 목록 조회와 같다:
     * 조인이 있어야 @SQLRestriction이 걸려 탈퇴한 계정이 카운트에서 빠지고, 그래야
     * followerCount와 팔로워 목록의 길이가 어긋나지 않는다.
     *
     * count(f)가 아니라 count(follower)인 것이 핵심이다. 조인해둔 별칭을 select에서
     * 쓰지 않으면 하이버네이트가 그 조인을 통째로 지워버리고(FK 컬럼만 보면 되므로)
     * users 테이블이 쿼리에서 사라져 @SQLRestriction도 같이 사라진다. 실제로
     * count(f)로 두면 탈퇴한 팔로워가 카운트에는 남고 목록에서는 빠져서 숫자가 어긋난다.
     */
    @Query("select count(follower) from Follow f join f.follower follower where f.following.id = :userId")
    long countFollowersByFollowingId(@Param("userId") Long userId);

    @Query("select count(following) from Follow f join f.following following where f.follower.id = :userId")
    long countFollowingsByFollowerId(@Param("userId") Long userId);
}
