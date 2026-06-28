package com.algorythm.repository;

import com.algorythm.model.Composition;
import com.algorythm.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for compositions. Queries are scoped by owner so a user only ever
 * sees their own work.
 */
public interface CompositionRepository extends JpaRepository<Composition, Long> {

    List<Composition> findByOwnerOrderByUpdatedAtDesc(User owner);

    Optional<Composition> findByIdAndOwner(Long id, User owner);
}
