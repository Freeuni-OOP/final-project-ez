package com.algorythm.service;

import com.algorythm.dto.PublicCompositionResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.CompositionLike;
import com.algorythm.model.CompositionLikeId;
import com.algorythm.model.User;
import com.algorythm.repository.CompositionLikeRepository;
import com.algorythm.repository.CompositionRepository;
import com.algorythm.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.algorythm.model.NotificationType;
/**
 * Likes on compositions. Handles like/unlike (idempotent) and enriches public
 * composition responses with the like count and whether the viewer liked them.
 */
@Service
public class LikeService {

    private final CompositionLikeRepository likes;
    private final CompositionRepository compositions;
    private final UserRepository users;
    private final NotificationService notifications;

    public LikeService(CompositionLikeRepository likes,
                       CompositionRepository compositions,
                       UserRepository users,
                       NotificationService notifications) {
        this.likes = likes;
        this.compositions = compositions;
        this.users = users;
        this.notifications = notifications;
    }

    /** Like a public composition. Liking again is a no-op. */
    @Transactional
    public void like(String username, Long compositionId) {
        User user = requireUser(username);
        Composition composition = requirePublic(compositionId);
        CompositionLikeId id = new CompositionLikeId(user.getId(), composition.getId());
        if (!likes.existsById(id)) {
            likes.save(new CompositionLike(user.getId(), composition.getId()));
            notifications.notify(
                    composition.getOwner(),
                    user,
                    NotificationType.LIKE,
                    composition,
                    null
            );
        }
    }

    /** Remove a like. Unliking something that wasn't liked is a no-op. */
    @Transactional
    public void unlike(String username, Long compositionId) {
        User user = requireUser(username);
        likes.deleteByIdUserIdAndIdCompositionId(user.getId(), compositionId);
    }

    /** Build a response for one composition, filling in likeCount and likedByMe. */
    @Transactional(readOnly = true)
    public PublicCompositionResponse toResponse(Composition composition, String viewerUsername) {
        Long viewerId = viewerId(viewerUsername);
        long count = likes.countByIdCompositionId(composition.getId());
        boolean likedByMe = viewerId != null
                && likes.existsByIdUserIdAndIdCompositionId(viewerId, composition.getId());
        return PublicCompositionResponse.from(composition, count, likedByMe);
    }

    /** Build responses for a list of compositions using batched like lookups. */
    @Transactional(readOnly = true)
    public List<PublicCompositionResponse> toResponses(List<Composition> list, String viewerUsername) {
        Long viewerId = viewerId(viewerUsername);
        List<Long> ids = list.stream().map(Composition::getId).toList();
        Map<Long, Long> counts = countsByComposition(ids);
        Set<Long> liked = likedIds(viewerId, ids);
        return list.stream()
                .map(c -> PublicCompositionResponse.from(
                        c,
                        counts.getOrDefault(c.getId(), 0L),
                        liked.contains(c.getId())))
                .toList();
    }

    private Map<Long, Long> countsByComposition(List<Long> ids) {
        Map<Long, Long> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Object[] row : likes.countByCompositionIdIn(ids)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Set<Long> likedIds(Long viewerId, List<Long> ids) {
        if (viewerId == null || ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(likes.likedCompositionIds(viewerId, ids));
    }

    private Long viewerId(String username) {
        if (username == null) {
            return null;
        }
        return users.findByUsername(username).map(User::getId).orElse(null);
    }

    private User requireUser(String username) {
        return users.findByUsername(username).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private Composition requirePublic(Long id) {
        Composition composition = compositions.findById(id).orElse(null);
        if (composition == null || !composition.isPublic()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Composition not found");
        }
        return composition;
    }
}
