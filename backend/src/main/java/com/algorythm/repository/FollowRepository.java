package com.algorythm.repository;

import com.algorythm.model.Follow;
import com.algorythm.model.FollowId;
import com.algorythm.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for follows: existence/counts, the followed-ids lookup, and lists. */
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    void deleteByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    /** How many people this user follows. */
    long countByIdFollowerId(Long followerId);

    /** How many people follow this user. */
    long countByIdFollowingId(Long followingId);

    /** Ids of everyone this user follows (for the following feed). */
    @Query("select f.id.followingId from Follow f where f.id.followerId = :followerId")
    List<Long> findFollowingIds(@Param("followerId") Long followerId);

    /** The users who follow the given user, most recent first. */
    @Query("select u from User u, Follow f where f.id.followerId = u.id "
            + "and f.id.followingId = :userId order by f.createdAt desc")
    List<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    /** The users the given user follows, most recent first. */
    @Query("select u from User u, Follow f where f.id.followingId = u.id "
            + "and f.id.followerId = :userId order by f.createdAt desc")
    List<User> findFollowing(@Param("userId") Long userId, Pageable pageable);

    /** Of the given user ids, the ones the viewer already follows (batch is-following). */
    @Query("select f.id.followingId from Follow f where f.id.followerId = :viewerId "
            + "and f.id.followingId in :ids")
    List<Long> followedAmong(@Param("viewerId") Long viewerId, @Param("ids") List<Long> ids);
}
