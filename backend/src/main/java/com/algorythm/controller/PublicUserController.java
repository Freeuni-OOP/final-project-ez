package com.algorythm.controller;

import com.algorythm.dto.UserProfileResponse;
import com.algorythm.dto.UserSummaryResponse;
import com.algorythm.security.ViewerResolver;
import com.algorythm.service.UserDirectoryService;
import com.algorythm.service.UserProfileService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public user profiles, search, and follower/following lists. Under /api/public/**
 * (no auth required); a token is optional and, when present, fills in isFollowing.
 */
@RestController
@RequestMapping("/api/public/users")
public class PublicUserController {

    private final UserProfileService userProfileService;
    private final UserDirectoryService userDirectoryService;
    private final ViewerResolver viewerResolver;

    public PublicUserController(
            UserProfileService userProfileService,
            UserDirectoryService userDirectoryService,
            ViewerResolver viewerResolver) {
        this.userProfileService = userProfileService;
        this.userDirectoryService = userDirectoryService;
        this.viewerResolver = viewerResolver;
    }

    /** Search users by username. */
    @GetMapping
    public List<UserSummaryResponse> search(Authentication authentication, @RequestParam String q) {
        return userDirectoryService.search(q, viewerResolver.username(authentication));
    }

    @GetMapping("/{username}")
    public UserProfileResponse get(Authentication authentication, @PathVariable String username) {
        return userProfileService.getProfile(username, viewerResolver.username(authentication));
    }

    @GetMapping("/{username}/followers")
    public List<UserSummaryResponse> followers(
            Authentication authentication, @PathVariable String username) {
        return userDirectoryService.followers(username, viewerResolver.username(authentication));
    }

    @GetMapping("/{username}/following")
    public List<UserSummaryResponse> following(
            Authentication authentication, @PathVariable String username) {
        return userDirectoryService.following(username, viewerResolver.username(authentication));
    }
}
