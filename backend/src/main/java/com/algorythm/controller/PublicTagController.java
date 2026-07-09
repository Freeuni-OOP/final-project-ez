package com.algorythm.controller;

import com.algorythm.service.TagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read access to the tag vocabulary. Under /api/public/** (no auth
 * required) — backs the explore side's tag filter UI with only the tags that
 * currently have at least one published composition.
 */
@RestController
@RequestMapping("/api/public/tags")
public class PublicTagController {

    private final TagService tagService;

    public PublicTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<String> list() {
        return tagService.publicTagNames();
    }
}