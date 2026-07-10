package com.algorythm.service;

import com.algorythm.dto.NotificationResponse;
import com.algorythm.model.Composition;
import com.algorythm.model.Notification;
import com.algorythm.model.NotificationType;
import com.algorythm.model.User;
import com.algorythm.repository.NotificationRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class NotificationService {

    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void notify(
            User recipient,
            User actor,
            NotificationType type,
            Composition composition,
            Long commentId
    ) {
        // Never notify users about their own actions.
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        notifications.save(
                new Notification(
                        recipient,
                        actor,
                        type,
                        composition,
                        commentId
                )
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recent(String username, int limit) {
        return notifications
                .findByRecipientUsernameOrderByCreatedAtDesc(
                        username,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        return notifications.countByRecipientUsernameAndReadFalse(username);
    }

    @Transactional
    public void markRead(Long id, String username) {
        Notification notification =
                notifications
                        .findByIdAndRecipientUsername(id, username)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification not found"));

        notification.markRead();
    }
}