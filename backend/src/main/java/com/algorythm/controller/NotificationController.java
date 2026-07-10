package com.algorythm.controller;

import com.algorythm.dto.NotificationResponse;
import com.algorythm.service.NotificationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The current user's notifications: recent list, unread count, mark-read. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    /** Bounds on the page size a caller can ask for. */
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> recent(
            Principal principal, @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));
        return notifications.recent(principal.getName(), safeLimit);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Principal principal) {
        return notifications.unreadCount(principal.getName());
    }

    @PostMapping("/{id}/read")
    public void markRead(Principal principal, @PathVariable Long id) {
        notifications.markRead(id, principal.getName());
    }
}
