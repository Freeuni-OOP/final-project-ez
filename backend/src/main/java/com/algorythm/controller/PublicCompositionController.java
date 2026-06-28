package com.algorythm.controller;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.service.PublicCompositionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read access to published compositions. Lives under /api/public/** which
 * the security config leaves open (no token required) — the explore feed and
 * share links.
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicCompositionService.feed(page, size);
    }

    @GetMapping("/{slug}")
    public PublicCompositionResponse get(@PathVariable String slug) {
        return publicCompositionService.getBySlug(slug);
    }
}
