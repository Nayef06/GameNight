package com.gamenight.dto;

import com.gamenight.model.GamingPlatform;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class LibraryDtos {
    private LibraryDtos() {}

    public record AddGameRequest(@NotNull(message = "must be provided") Long gameId,
                                 GamingPlatform platform) {}

    public record LibraryGameResponse(Long id, String title, List<String> tags,
                                      boolean multiplayerSupport, int maxPlayers,
                                      GamingPlatform platform) {}

    public record GameResponse(
            Long id,
            String title,
            List<String> tags,
            boolean multiplayerSupport,
            int maxPlayers
    ) {}
}
