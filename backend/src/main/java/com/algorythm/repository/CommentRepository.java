package com.algorythm.repository;

import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for comments. */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByCompositionOrderByCreatedAtAsc(Composition composition);
}
