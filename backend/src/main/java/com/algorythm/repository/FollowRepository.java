package com.algorythm.repository;

import com.algorythm.model.Follow;
import com.algorythm.model.FollowId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for follows: existence/counts plus the followed-ids lookup. */
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
}
