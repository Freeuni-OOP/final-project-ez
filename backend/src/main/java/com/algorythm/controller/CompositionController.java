package com.algorythm.controller;

import com.algorythm.dto.CompositionRequest;
import com.algorythm.dto.CompositionResponse;
import com.algorythm.service.CompositionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD + publish/unpublish for the current user's compositions. Lives under
 * /api/** so the security config requires a valid JWT; the username comes from
 * the authenticated token.
 */
@RestController
@RequestMapping("/api/compositions")
public class CompositionController {

    private final CompositionService compositionService;

    public CompositionController(CompositionService compositionService) {
        this.compositionService = compositionService;
    }

    @GetMapping
    public List<CompositionResponse> list(Authentication authentication) {
        return compositionService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public CompositionResponse get(Authentication authentication, @PathVariable Long id) {
        return compositionService.get(authentication.getName(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompositionResponse create(
            Authentication authentication, @Valid @RequestBody CompositionRequest request) {
        return compositionService.create(authentication.getName(), request);
    }

    @PutMapping("/{id}")
    public CompositionResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CompositionRequest request) {
        return compositionService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        compositionService.delete(authentication.getName(), id);
    }

    @PostMapping("/{id}/publish")
    public CompositionResponse publish(Authentication authentication, @PathVariable Long id) {
        return compositionService.publish(authentication.getName(), id);
    }

    @PostMapping("/{id}/unpublish")
    public CompositionResponse unpublish(Authentication authentication, @PathVariable Long id) {
        return compositionService.unpublish(authentication.getName(), id);
    }
}
