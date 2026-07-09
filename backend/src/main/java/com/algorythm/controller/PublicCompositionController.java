package com.algorythm.controller;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.service.PublicCompositionService;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read access to published compositions. Lives under /api/public/** which
 * the security config leaves open (no token required) — the explore feed
 * (optionally filtered by tag), text search, and share links. A token is
 * optional here: if one is sent, likedByMe reflects that user; if not,
 * likedByMe is false.
 */
@RestController
@RequestMapping("/api/public/compositions")
public class PublicCompositionController {

    private final PublicCompositionService publicCompositionService;

    public PublicCompositionController(PublicCompositionService publicCompositionService) {
        this.publicCompositionService = publicCompositionService;
    }

    @GetMapping
    public List<PublicCompositionResponse> feed(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tag) {
        return publicCompositionService.feed(page, size, tag, viewer(authentication));
    }

    @GetMapping("/search")
    public List<PublicCompositionResponse> search(
            Authentication authentication,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicCompositionService.search(q, page, size, viewer(authentication));
    }

    @GetMapping("/{slug}")
    public PublicCompositionResponse get(Authentication authentication, @PathVariable String slug) {
        return publicCompositionService.getBySlug(slug, viewer(authentication));
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