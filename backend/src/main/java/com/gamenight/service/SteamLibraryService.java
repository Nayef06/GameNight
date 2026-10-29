package com.gamenight.service;

import com.gamenight.dto.SteamDtos.SteamConnectionResponse;
import com.gamenight.dto.SteamDtos.SteamSyncResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.UserGameRepository;
import com.gamenight.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class SteamLibraryService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    private final SteamOwnedGamesClient steamClient;

    public SteamLibraryService(CurrentUserService currentUserService, UserRepository userRepository,
                               GameRepository gameRepository, UserGameRepository userGameRepository,
                               SteamOwnedGamesClient steamClient) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
        this.steamClient = steamClient;
    }

    @Transactional(readOnly = true)
    public SteamConnectionResponse getConnection() {
        return new SteamConnectionResponse(currentUserService.get().getSteamId());
    }

    @Transactional
    public SteamConnectionResponse saveSteamId(String requestedSteamId) {
        String steamId = requestedSteamId == null ? "" : requestedSteamId.trim();
        if (!steamId.matches("7656119[0-9]{10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Steam ID.");
        }
        AppUser user = currentUserService.get();
        user.setSteamId(steamId);
        userRepository.save(user);
        return new SteamConnectionResponse(steamId);
    }

    @Transactional
    public SteamSyncResponse sync() {
        AppUser user = currentUserService.get();
        if (user.getSteamId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Save a Steam ID first.");
        }
        var ownedGames = steamClient.getOwnedGames(user.getSteamId());
        int imported = 0;
        int alreadyOwned = 0;
        Set<Long> seenAppIds = new HashSet<>();
        for (var ownedGame : ownedGames) {
            if (!seenAppIds.add(ownedGame.appId())) {
                continue;
            }
            String title = ownedGame.name().trim();
            String normalizedTitle = title.toLowerCase(Locale.ROOT);
            if (title.length() > 120) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Unable to retrieve Steam library.");
            }
            Game game = gameRepository.findBySteamAppId(ownedGame.appId()).orElse(null);
            if (game == null) {
                game = gameRepository.findByNormalizedTitle(normalizedTitle).orElse(null);
                if (game == null) {
                    game = new Game(title, normalizedTitle, "Other", false, 1);
                }
                if (game.getSteamAppId() == null) {
                    game.setSteamAppId(ownedGame.appId());
                }
                game = gameRepository.save(game);
            }
            if (userGameRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
                alreadyOwned++;
            } else {
                userGameRepository.save(new UserGame(user, game));
                imported++;
            }
        }
        return new SteamSyncResponse(imported, alreadyOwned);
    }
}
