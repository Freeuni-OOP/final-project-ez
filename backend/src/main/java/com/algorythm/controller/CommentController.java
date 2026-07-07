package com.algorythm.controller;

import com.algorythm.dto.CommentRequest;
import com.algorythm.dto.CommentResponse;
import com.algorythm.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated comment actions. Both routes live under /api/** so the security
 * config requires a valid JWT; the username comes from the token.
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/compositions/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        return commentService.add(authentication.getName(), id, request);
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        commentService.delete(authentication.getName(), id);
    }
}
