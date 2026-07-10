package com.algorythm.controller;

import com.algorythm.dto.UserProfileResponse;
import com.algorythm.security.ViewerResolver;
import com.algorythm.service.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public user profiles. Under /api/public/** (no auth required) — returns a user's
 * public info plus their published compositions. A token is optional: if sent,
 * likedByMe on each composition reflects that user.
 */
@RestController
@RequestMapping("/api/public/users")
public class PublicUserController {

    private final UserProfileService userProfileService;
    private final ViewerResolver viewerResolver;

    public PublicUserController(
            UserProfileService userProfileService, ViewerResolver viewerResolver) {
        this.userProfileService = userProfileService;
        this.viewerResolver = viewerResolver;
    }

    @GetMapping("/{username}")
    public UserProfileResponse get(Authentication authentication, @PathVariable String username) {
        return userProfileService.getProfile(username, viewerResolver.username(authentication));
    }
}
