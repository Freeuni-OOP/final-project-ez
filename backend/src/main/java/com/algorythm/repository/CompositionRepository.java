package com.algorythm.repository;

import com.algorythm.model.Composition;
import com.algorythm.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for compositions. Owner-scoped lookups back the private CRUD;
 * the public methods back the explore feed and share links.
 */
public interface CompositionRepository extends JpaRepository<Composition, Long> {

    List<Composition> findByOwnerOrderByUpdatedAtDesc(User owner);

    Optional<Composition> findByIdAndOwner(Long id, User owner);

    boolean existsBySlug(String slug);

    // --- public reads ---

    Optional<Composition> findBySlugAndIsPublicTrue(String slug);

    List<Composition> findByIsPublicTrueOrderByUpdatedAtDesc(Pageable pageable);
}
