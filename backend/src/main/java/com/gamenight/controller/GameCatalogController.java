package com.gamenight.controller;

import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.service.GameCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameCatalogController {
    private final GameCatalog gameCatalog;

    public GameCatalogController(GameCatalog gameCatalog) {
        this.gameCatalog = gameCatalog;
    }

    @GetMapping
    public List<GameResponse> getGames() {
        return gameCatalog.getGames();
    }

    @GetMapping("/tags")
    public List<String> getTags() {
        return gameCatalog.getTags();
    }
}
