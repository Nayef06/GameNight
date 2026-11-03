package com.gamenight.dto;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(Long id, String message, Instant createdAt, boolean isRead) {}
}
