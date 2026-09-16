package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class LibraryDtos {
    private LibraryDtos() {}

    public record AddGameRequest(@NotBlank @Size(max = 120) String title) {}
    public record GameResponse(Long id, String title) {}
}

