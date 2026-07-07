package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.dto.UserProfileResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds a public profile: the user's public info, follow stats, and their
 * published compositions.
 */
@Service
public class UserProfileService {

    private final UserRepository users;
    private final CompositionRepository compositions;
    private final LikeService likeService;
    private final FollowService followService;

    public UserProfileService(UserRepository users,
                              CompositionRepository compositions,
                              LikeService likeService,
                              FollowService followService) {
        this.users = users;
        this.compositions = compositions;
        this.likeService = likeService;
        this.followService = followService;
    }

    /** viewerUsername is the logged-in user, or null for anonymous requests. */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, String viewerUsername) {
        User user = users.findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<Composition> list =
                compositions.findByOwnerAndIsPublicTrueOrderByUpdatedAtDesc(user);
        List<PublicCompositionResponse> published = likeService.toResponses(list, viewerUsername);

        long followerCount = followService.followerCount(user.getId());
        long followingCount = followService.followingCount(user.getId());
        boolean isFollowing = followService.isFollowing(viewerId(viewerUsername), user.getId());

        return new UserProfileResponse(
                user.getUsername(),
                user.getCreatedAt(),
                followerCount,
                followingCount,
                isFollowing,
                published);
    }

    private Long viewerId(String viewerUsername) {
        if (viewerUsername == null) {
            return null;
        }
        return users.findByUsername(viewerUsername).map(User::getId).orElse(null);
    }
}
