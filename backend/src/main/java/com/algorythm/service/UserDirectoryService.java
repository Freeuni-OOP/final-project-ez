package com.algorythm.service;

import com.algorythm.dto.UserSummaryResponse;
import com.algorythm.model.User;
import com.algorythm.repository.FollowRepository;
import com.algorythm.repository.UserRepository;
import com.algorythm.security.ViewerResolver;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Finding people: username search, and a user's followers / following lists. Each
 * result carries whether the current viewer already follows that user, so the UI
 * can show a follow button without extra round-trips.
 */
@Service
public class UserDirectoryService {

    private static final int MAX_RESULTS = 50;

    private final UserRepository users;
    private final FollowRepository follows;
    private final ViewerResolver viewerResolver;

    public UserDirectoryService(
            UserRepository users, FollowRepository follows, ViewerResolver viewerResolver) {
        this.users = users;
        this.follows = follows;
        this.viewerResolver = viewerResolver;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> search(String query, String viewerUsername) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<User> found = users.findByUsernameContainingIgnoreCaseOrderByUsernameAsc(
                query.trim(), PageRequest.of(0, MAX_RESULTS));
        return toSummaries(found, viewerUsername);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> followers(String username, String viewerUsername) {
        User user = requireUser(username);
        return toSummaries(
                follows.findFollowers(user.getId(), PageRequest.of(0, MAX_RESULTS)), viewerUsername);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> following(String username, String viewerUsername) {
        User user = requireUser(username);
        return toSummaries(
                follows.findFollowing(user.getId(), PageRequest.of(0, MAX_RESULTS)), viewerUsername);
    }

    private List<UserSummaryResponse> toSummaries(List<User> found, String viewerUsername) {
        Set<Long> followed = followedAmong(viewerResolver.resolveId(viewerUsername), found);
        return found.stream()
                .map(u -> UserSummaryResponse.of(u, followed.contains(u.getId())))
                .toList();
    }

    private Set<Long> followedAmong(Long viewerId, List<User> found) {
        if (viewerId == null || found.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = found.stream().map(User::getId).toList();
        return new HashSet<>(follows.followedAmong(viewerId, ids));
    }

    private User requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
