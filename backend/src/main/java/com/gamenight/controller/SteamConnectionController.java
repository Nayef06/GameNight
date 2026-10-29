package com.gamenight.controller;

import com.gamenight.dto.SteamDtos.SaveSteamIdRequest;
import com.gamenight.dto.SteamDtos.SteamConnectionResponse;
import com.gamenight.service.SteamLibraryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/steam")
public class SteamConnectionController {
    private final SteamLibraryService steamLibraryService;

    public SteamConnectionController(SteamLibraryService steamLibraryService) {
        this.steamLibraryService = steamLibraryService;
    }

    @GetMapping
    public SteamConnectionResponse getConnection() {
        return steamLibraryService.getConnection();
    }

    @PutMapping
    public SteamConnectionResponse saveSteamId(@Valid @RequestBody SaveSteamIdRequest request) {
        return steamLibraryService.saveSteamId(request.steamId());
    }
}
