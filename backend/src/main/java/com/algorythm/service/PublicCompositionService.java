package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.repository.CompositionRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only access to published compositions (no auth, no owner scoping) -
 * the explore feed (optionally filtered by tag), text search, and share
 * links.
 */
@Service
public class PublicCompositionService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CompositionRepository compositions;
    private final LikeService likeService;

    public PublicCompositionService(CompositionRepository compositions, LikeService likeService) {
        this.compositions = compositions;
        this.likeService = likeService;
    }

    /** viewerUsername is the logged-in user, or null for anonymous requests. */
    @Transactional(readOnly = true)
    public PublicCompositionResponse getBySlug(String slug, String viewerUsername) {
        Composition composition = compositions.findBySlugAndIsPublicTrue(slug)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
        return likeService.toResponse(composition, viewerUsername);
    }

    /**
     * The explore feed: public compositions, newest first, paginated. When
     * tag is given (and non-blank), narrows to compositions carrying that
     * tag - matched the same normalized way tags are stored.
     */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> feed(
            int page, int size, String tag, String viewerUsername) {
        Pageable pageable = pageable(page, size);
        String normalizedTag = TagService.normalize(tag);
        List<Composition> list = normalizedTag.isEmpty()
                ? compositions.findByIsPublicTrueOrderByUpdatedAtDesc(pageable)
                : compositions.findByIsPublicTrueAndTagName(normalizedTag, pageable);
        return likeService.toResponses(list, viewerUsername);
    }

    /** Text search across public compositions' titles and tags, newest first, paginated. */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> search(String q, int page, int size, String viewerUsername) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        List<Composition> list =
                compositions.searchPublicByTitleOrTag(q.trim(), pageable(page, size));
        return likeService.toResponses(list, viewerUsername);
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}