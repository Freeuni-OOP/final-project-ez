package com.algorythm.service;

import com.algorythm.dto.CompositionRequest;
import com.algorythm.dto.CompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Composition CRUD + publish/unpublish, always scoped to the current user.
 * Lookups go through findByIdAndOwner so one user can never read or modify
 * another user's work (a miss returns 404 rather than revealing the id exists).
 */
@Service
public class CompositionService {

    private static final String SLUG_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SLUG_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CompositionRepository compositions;
    private final UserRepository users;

    public CompositionService(CompositionRepository compositions, UserRepository users) {
        this.compositions = compositions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CompositionResponse> list(String username) {
        User owner = currentUser(username);
        return compositions.findByOwnerOrderByUpdatedAtDesc(owner).stream()
                .map(CompositionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompositionResponse get(String username, Long id) {
        return CompositionResponse.from(owned(username, id));
    }

    @Transactional
    public CompositionResponse create(String username, CompositionRequest request) {
        User owner = currentUser(username);
        Composition saved = compositions.save(
                new Composition(owner, request.title(), request.pattern(), request.bpm()));
        return CompositionResponse.from(saved);
    }

    @Transactional
    public CompositionResponse update(String username, Long id, CompositionRequest request) {
        Composition composition = owned(username, id);
        composition.setTitle(request.title());
        composition.setPattern(request.pattern());
        composition.setBpm(request.bpm());
        return CompositionResponse.from(composition); // managed entity flushes on commit
    }

    @Transactional
    public void delete(String username, Long id) {
        compositions.delete(owned(username, id));
    }

    /** Make a composition public, assigning a stable share slug on first publish. */
    @Transactional
    public CompositionResponse publish(String username, Long id) {
        Composition composition = owned(username, id);
        if (composition.getSlug() == null) {
            composition.setSlug(uniqueSlug());
        }
        composition.setPublic(true);
        return CompositionResponse.from(composition);
    }

    /** Make a composition private again (the slug is kept for re-publishing). */
    @Transactional
    public CompositionResponse unpublish(String username, Long id) {
        Composition composition = owned(username, id);
        composition.setPublic(false);
        return CompositionResponse.from(composition);
    }

    /** The composition with this id owned by the user, or 404. */
    private Composition owned(String username, Long id) {
        return compositions.findByIdAndOwner(id, currentUser(username))
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
    }

    private User currentUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private String uniqueSlug() {
        String slug;
        do {
            slug = randomSlug();
        } while (compositions.existsBySlug(slug));
        return slug;
    }

    private static String randomSlug() {
        StringBuilder sb = new StringBuilder(SLUG_LENGTH);
        for (int i = 0; i < SLUG_LENGTH; i++) {
            sb.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return sb.toString();
    }
}
