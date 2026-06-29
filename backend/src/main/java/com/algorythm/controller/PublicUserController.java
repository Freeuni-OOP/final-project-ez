package com.algorythm.controller;

import com.algorythm.dto.UserProfileResponse;
import com.algorythm.service.UserProfileService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    public PublicUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{username}")
    public UserProfileResponse get(Authentication authentication, @PathVariable String username) {
        return userProfileService.getProfile(username, viewer(authentication));
    }

    /** Logged-in username, or null when the request is anonymous. */
    private static String viewer(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
