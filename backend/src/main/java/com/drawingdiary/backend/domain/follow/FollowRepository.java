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
}
