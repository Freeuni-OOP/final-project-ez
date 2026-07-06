package com.algorythm.service;

import com.algorythm.dto.CommentRequest;
import com.algorythm.dto.CommentResponse;
import com.algorythm.model.Comment;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CommentRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Comments on compositions: public reads by slug, authenticated posting, and
 * author-only deletion.
 */
@Service
public class CommentService {

    private final CommentRepository comments;
    private final CompositionRepository compositions;
    private final UserRepository users;

    public CommentService(CommentRepository comments,
                          CompositionRepository compositions,
                          UserRepository users) {
        this.comments = comments;
        this.compositions = compositions;
        this.users = users;
    }

    /** Comments on a public composition, oldest first. No auth required. */
    @Transactional(readOnly = true)
    public List<CommentResponse> listForPublicComposition(String slug) {
        Composition composition = compositions.findBySlugAndIsPublicTrue(slug)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
        return comments.findByCompositionOrderByCreatedAtAsc(composition).stream()
                .map(CommentResponse::from)
                .toList();
    }

    /** Post a comment on a public composition. */
    @Transactional
    public CommentResponse add(String username, Long compositionId, CommentRequest request) {
        User author = currentUser(username);
        Composition composition = publicComposition(compositionId);
        Comment saved = comments.save(new Comment(composition, author, request.body()));
        return CommentResponse.from(saved);
    }

    /** Delete a comment. Only its author may do so. */
    @Transactional
    public void delete(String username, Long commentId) {
        User user = currentUser(username);
        Comment comment = comments.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your comment");
        }
        comments.delete(comment);
    }

    private Composition publicComposition(Long id) {
        Composition composition = compositions.findById(id).orElse(null);
        if (composition == null || !composition.isPublic()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found");
        }
        return composition;
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }
}
