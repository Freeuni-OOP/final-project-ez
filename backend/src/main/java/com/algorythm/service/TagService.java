package com.algorythm.service;

import com.algorythm.model.Tag;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.TagRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Normalizes and resolves the tags a composition's creator attaches (get the
 * existing Tag row for a name, or create it) so the same word is always
 * shared rather than duplicated under different casing/whitespace.
 */
@Service
public class TagService {

    static final int MAX_TAGS_PER_COMPOSITION = 5;
    static final int MAX_TAG_LENGTH = 30;

    private final TagRepository tags;
    private final CompositionRepository compositions;

    public TagService(TagRepository tags, CompositionRepository compositions) {
        this.tags = tags;
        this.compositions = compositions;
    }

    /** Normalizes, validates, and get-or-creates a Tag for each distinct name. */
    @Transactional
    public Set<Tag> resolve(List<String> rawNames) {
        if (rawNames == null) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String raw : rawNames) {
            String normalized = normalize(raw);
            if (!normalized.isEmpty()) {
                names.add(normalized);
            }
        }
        if (names.size() > MAX_TAGS_PER_COMPOSITION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A composition can have at most " + MAX_TAGS_PER_COMPOSITION + " tags");
        }
        Set<Tag> resolved = new LinkedHashSet<>();
        for (String name : names) {
            if (name.length() > MAX_TAG_LENGTH) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tag \"" + name + "\" is too long (max " + MAX_TAG_LENGTH + " characters)");
            }
            resolved.add(findOrCreate(name));
        }
        return resolved;
    }

    /** Every tag name currently used by at least one published composition. */
    @Transactional(readOnly = true)
    public List<String> publicTagNames() {
        return compositions.findDistinctTagNamesForPublicCompositions();
    }

    private Tag findOrCreate(String name) {
        return tags.findByName(name).orElseGet(() -> {
            try {
                return tags.save(new Tag(name));
            } catch (DataIntegrityViolationException raceLostToAnotherRequest) {
                return tags.findByName(name).orElseThrow(() -> raceLostToAnotherRequest);
            }
        });
    }

    /** Lowercased, trimmed, internal whitespace collapsed - so "Lo-Fi " and "lo-fi" match. */
    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}