package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class LibraryDtos {
    private LibraryDtos() {}

    public record AddGameRequest(
            @NotBlank(message = "cannot be blank") @Size(max = 120) String title,
            @NotBlank(message = "cannot be blank") @Size(max = 50) String genre,
            @NotNull(message = "must have a value") Boolean multiplayerSupport,
            @NotNull(message = "must have a value") @Min(value = 1, message = "must be at least 1") Integer maxPlayers
    ) {}

    public record GameResponse(
            Long id,
            String title,
            String genre,
            boolean multiplayerSupport,
            int maxPlayers
    ) {}
}
