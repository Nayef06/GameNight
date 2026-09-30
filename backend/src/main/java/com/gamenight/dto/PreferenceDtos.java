package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public final class PreferenceDtos {
    private PreferenceDtos() {}

    public record UpdatePreferencesRequest(
            @NotNull(message = "must be provided") List<String> preferredGenres,
            @NotBlank(message = "must be provided") String multiplayerPreference,
            @Positive(message = "must be positive") Integer preferredPlayerCount
    ) {}

    public record PreferencesResponse(
            List<String> preferredGenres,
            String multiplayerPreference,
            Integer preferredPlayerCount
    ) {}
}
