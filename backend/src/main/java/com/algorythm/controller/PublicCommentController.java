package com.algorythm.controller;

import com.algorythm.dto.CommentResponse;
import com.algorythm.service.CommentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read access to a composition's comments. Under /api/public/** which the
 * security config leaves open (no token required).
 */
@RestController
@RequestMapping("/api/public/compositions")
public class PublicCommentController {

    private final CommentService commentService;

    public PublicCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/{slug}/comments")
    public List<CommentResponse> list(@PathVariable String slug) {
        return commentService.listForPublicComposition(slug);
    }
}
