package com.gamenight.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SessionDtos {
    private SessionDtos() {}

    public record CreateSessionRequest(@NotNull @Size(min = 2, max = 5) List<@NotNull Long> gameIds) {}
    public record CandidateGame(Long id, String title) {}
    public record SessionResponse(Long id, Long groupId, String createdBy,
                                  Instant createdAt, String status, List<CandidateGame> candidates) {}
}
