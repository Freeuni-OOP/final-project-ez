package com.algorythm.repository;

import com.algorythm.model.Tag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for tags. Lookups back TagService's get-or-create logic. */
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}