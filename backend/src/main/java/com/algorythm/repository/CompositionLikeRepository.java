package com.algorythm.repository;

import com.algorythm.model.CompositionLike;
import com.algorythm.model.CompositionLikeId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for likes: single checks/counts plus batch lookups for the feed. */
public interface CompositionLikeRepository
        extends JpaRepository<CompositionLike, CompositionLikeId> {

    long countByIdCompositionId(Long compositionId);

    boolean existsByIdUserIdAndIdCompositionId(Long userId, Long compositionId);

    void deleteByIdUserIdAndIdCompositionId(Long userId, Long compositionId);

    /** Like counts for several compositions at once: rows of [compositionId, count]. */
    @Query("select cl.id.compositionId, count(cl) from CompositionLike cl "
            + "where cl.id.compositionId in :ids group by cl.id.compositionId")
    List<Object[]> countByCompositionIdIn(@Param("ids") List<Long> ids);

    /** Of the given compositions, the ids this user has liked. */
    @Query("select cl.id.compositionId from CompositionLike cl "
            + "where cl.id.userId = :userId and cl.id.compositionId in :ids")
    List<Long> likedCompositionIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}
