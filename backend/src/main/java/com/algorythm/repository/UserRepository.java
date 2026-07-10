package com.algorythm.repository;

import com.algorythm.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for users. Lookups here back login (find by username/email),
 * registration (uniqueness checks), and the public user search.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Users whose username contains the query (case-insensitive), for user search. */
    List<User> findByUsernameContainingIgnoreCaseOrderByUsernameAsc(String query, Pageable pageable);
}
