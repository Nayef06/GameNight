package com.gamenight.controller;

import com.gamenight.dto.LibraryDtos.AddGameRequest;
import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.dto.SteamDtos.SteamSyncResponse;
import com.gamenight.service.LibraryService;
import com.gamenight.service.SteamLibraryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {
    private final LibraryService libraryService;
    private final SteamLibraryService steamLibraryService;

    public LibraryController(LibraryService libraryService, SteamLibraryService steamLibraryService) {
        this.libraryService = libraryService;
        this.steamLibraryService = steamLibraryService;
    }

    @GetMapping
    public List<GameResponse> getLibrary() {
        return libraryService.getLibrary();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GameResponse addGame(@Valid @RequestBody AddGameRequest request) {
        return libraryService.addGame(request.gameId());
    }

    @PostMapping("/steam/sync")
    public SteamSyncResponse syncSteamLibrary() {
        return steamLibraryService.sync();
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGame(@PathVariable Long gameId) {
        libraryService.removeGame(gameId);
    }
}
