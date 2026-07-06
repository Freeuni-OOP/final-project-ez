package com.algorythm.controller;

import com.algorythm.service.FollowService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Follow / unfollow a user. Requires authentication (under /api/**). */
@RestController
@RequestMapping("/api/users/{username}/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(Authentication authentication, @PathVariable String username) {
        followService.follow(authentication.getName(), username);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(Authentication authentication, @PathVariable String username) {
        followService.unfollow(authentication.getName(), username);
    }
}
