package com.gamenight.dto;

import jakarta.validation.constraints.NotBlank;

public final class SteamDtos {
    private SteamDtos() {}

    public record SaveSteamIdRequest(@NotBlank String steamId) {}
    public record SteamConnectionResponse(String steamId) {}
    public record SteamSyncResponse(int imported, int alreadyInLibrary) {}
}
