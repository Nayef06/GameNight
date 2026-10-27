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
    public record VoteRequest(@NotNull Long gameId) {}
    public record VoteCount(Long gameId, String title, long votes) {}
    public record VotingResultsResponse(String status, Long currentUserVoteGameId,
                                        List<VoteCount> results) {}
}
