package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The personalized feed: public compositions by the people a user follows. */
@Service
public class FeedService {

    private static final int MAX_PAGE_SIZE = 50;

    private final FollowRepository follows;
    private final CompositionRepository compositions;
    private final UserRepository users;
    private final LikeService likeService;

    public FeedService(FollowRepository follows,
                       CompositionRepository compositions,
                       UserRepository users,
                       LikeService likeService) {
        this.follows = follows;
        this.compositions = compositions;
        this.users = users;
        this.likeService = likeService;
    }

    /** Public compositions by everyone the user follows, newest first. */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> following(String username, int page, int size) {
        User viewer = users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        List<Long> followingIds = follows.findFollowingIds(viewer.getId());
        if (followingIds.isEmpty()) {
            return List.of();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        List<Composition> list = compositions.findByOwnerIdInAndIsPublicTrueOrderByUpdatedAtDesc(
                followingIds, PageRequest.of(safePage, safeSize));
        return likeService.toResponses(list, username);
    }
}
