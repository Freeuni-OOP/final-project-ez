package com.algorythm.controller;

import com.algorythm.service.LikeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Like / unlike a composition. Requires authentication (outside /api/public). */
@RestController
@RequestMapping("/api/compositions/{id}/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(Authentication authentication, @PathVariable Long id) {
        likeService.like(authentication.getName(), id);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(Authentication authentication, @PathVariable Long id) {
        likeService.unlike(authentication.getName(), id);
    }
}
