package com.algorythm.controller;

import com.algorythm.dto.NotificationResponse;
import com.algorythm.service.NotificationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationResponse> recent(
            Principal principal,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return notifications.recent(principal.getName(), Math.min(limit, 50));
    }

    @GetMapping("/unread-count")
    public long unreadCount(Principal principal) {
        return notifications.unreadCount(principal.getName());
    }

    @PostMapping("/{id}/read")
    public void markRead(
            Principal principal,
            @PathVariable Long id
    ) {
        notifications.markRead(id, principal.getName());
    }
}