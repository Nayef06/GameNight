package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class GroupDtos {
    private GroupDtos() {}

    public record CreateGroupRequest(@NotBlank @Size(max = 80) String name) {}
    public record JoinGroupRequest(@NotBlank @Size(min = 6, max = 6) String joinCode) {}
    public record GroupSummary(Long id, String name, String joinCode) {}
    public record GroupDetails(
            Long id,
            String name,
            String joinCode,
            String createdBy,
            Instant createdAt,
            List<String> members
    ) {}
}

