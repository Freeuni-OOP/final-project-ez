package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.repository.CompositionRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Read-only access to published compositions (no auth, no owner scoping). */
@Service
public class PublicCompositionService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CompositionRepository compositions;

    public PublicCompositionService(CompositionRepository compositions) {
        this.compositions = compositions;
    }

    @Transactional(readOnly = true)
    public PublicCompositionResponse getBySlug(String slug) {
        return compositions.findBySlugAndIsPublicTrue(slug)
                .map(PublicCompositionResponse::from)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found"));
    }

    /** The explore feed: public compositions, newest first, paginated. */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> feed(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return compositions
                .findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(safePage, safeSize))
                .stream()
                .map(PublicCompositionResponse::from)
                .toList();
    }
}
