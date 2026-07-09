package com.algorythm.repository;

import com.algorythm.model.Composition;
import com.algorythm.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for compositions. Owner-scoped lookups back the private CRUD;
 * the public methods back the explore feed, share links, profiles, and the
 * tag/search browsing on the explore side.
 */
public interface CompositionRepository extends JpaRepository<Composition, Long> {

    List<Composition> findByOwnerOrderByUpdatedAtDesc(User owner);

    Optional<Composition> findByIdAndOwner(Long id, User owner);

    boolean existsBySlug(String slug);

    // --- public reads ---

    Optional<Composition> findBySlugAndIsPublicTrue(String slug);

    List<Composition> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);

    /** A user's published compositions (for their public profile). */
    List<Composition> findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(User owner);

    /** Published compositions by a set of users (for the following feed). */
    List<Composition> findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(
            Collection<Long> ownerIds, Pageable pageable);

    /** The explore feed filtered to one tag (tag is already normalized by the caller). */
    @Query("select c from Composition c join c.tags t "
            + "where c.isPublic = true and t.name = :tag order by c.updatedAt desc")
    List<Composition> findByIsPublicTrueAndTagName(@Param("tag") String tag, Pageable pageable);

    /**
     * Text search across public compositions: matches the title or any tag
     * name. distinct avoids duplicate rows when several of a composition's
     * tags match.
     */
    @Query("select distinct c from Composition c left join c.tags t "
            + "where c.isPublic = true and ("
            + "lower(c.title) like lower(concat('%', :q, '%')) "
            + "or lower(t.name) like lower(concat('%', :q, '%'))) "
            + "order by c.updatedAt desc")
    List<Composition> searchPublicByTitleOrTag(@Param("q") String q, Pageable pageable);

    /** Every tag name used by at least one published composition, for the explore filter UI. */
    @Query("select distinct t.name from Composition c join c.tags t "
            + "where c.isPublic = true order by t.name")
    List<String> findDistinctTagNamesForPublicCompositions();
}