package com.algorythm.controller;

import com.algorythm.dto.UserProfileResponse;
import com.algorythm.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public user profiles. Under /api/public/** (no auth required) — returns a user's
 * public info plus their published compositions.
 */
@RestController
@RequestMapping("/api/public/users")
public class PublicUserController {

    private final UserProfileService userProfileService;

    public PublicUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{username}")
    public UserProfileResponse get(@PathVariable String username) {
        return userProfileService.getProfile(username);
    }
}
