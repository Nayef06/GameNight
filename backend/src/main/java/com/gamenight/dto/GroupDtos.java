package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class GroupDtos {
    private GroupDtos() {}

    public record CreateGroupRequest(@NotBlank @Size(max = 80) String name) {}
    public record JoinGroupRequest(@NotBlank @Size(min = 6, max = 6) String joinCode) {}
    public record CreateGameSelectionRequest(@NotNull Long gameId) {}
    public record GameSelectionResponse(Long id, Long gameId, String gameTitle, Instant selectedAt) {}
    public record GroupSummary(Long id, String name, String joinCode, long memberCount) {}
    public record GroupMember(Long id, String username, boolean creator) {}
    public record SharedGameResponse(
            Long id,
            String title,
            List<String> tags,
            boolean multiplayerSupport,
            int maxPlayers,
            int preferenceScore
    ) {}
    public record GroupDetails(
            Long id,
            String name,
            String joinCode,
            String createdBy,
            Instant createdAt,
            boolean currentUserIsCreator,
            List<GroupMember> members
    ) {}
}
