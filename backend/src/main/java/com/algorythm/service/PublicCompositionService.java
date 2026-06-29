package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
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

    /** The explore feed: public compositions, newest first, paginated. */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> feed(int page, int size, String viewerUsername) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<Composition> list = compositions
                .findByIsPublicTrueOrderByUpdatedAtDesc(PageRequest.of(safePage, safeSize));
        return likeService.toResponses(list, viewerUsername);
    }
}
