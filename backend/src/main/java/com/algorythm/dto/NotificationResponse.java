package com.algorythm.dto;

import com.algorythm.model.Notification;
import com.algorythm.model.NotificationType;
import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String actor,
        Long compositionId,
        String compositionTitle,
        String compositionSlug,
        Long commentId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        var composition = notification.getComposition();

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getActor().getUsername(),
                composition == null ? null : composition.getId(),
                composition == null ? null : composition.getTitle(),
                composition == null ? null : composition.getSlug(),
                notification.getCommentId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}