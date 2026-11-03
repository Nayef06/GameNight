package com.gamenight.service;

import com.gamenight.dto.LibraryDtos.LibraryGameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GamingPlatform;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.UserGameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LibraryService {
    private final CurrentUserService currentUserService;
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;

    public LibraryService(CurrentUserService currentUserService, GameRepository gameRepository,
                          UserGameRepository userGameRepository) {
        this.currentUserService = currentUserService;
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
    }

    @Transactional(readOnly = true)
    public List<LibraryGameResponse> getLibrary() {
        AppUser user = currentUserService.get();
        return userGameRepository.findByUserIdOrderByGameTitleAsc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LibraryGameResponse addGame(Long gameId, GamingPlatform requestedPlatform) {
        GamingPlatform platform = requestedPlatform == null ? GamingPlatform.OTHER : requestedPlatform;
        AppUser user = currentUserService.get();
        Game game = gameRepository.findById(gameId)
                .filter(candidate -> GameCatalog.contains(candidate.getNormalizedTitle()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a game from the catalog."));

        if (userGameRepository.existsByUserIdAndGameIdAndPlatform(user.getId(), game.getId(), platform)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already in your library on this platform.");
        }
        UserGame ownership = new UserGame(user, game, platform);
        userGameRepository.save(ownership);
        return toResponse(ownership);
    }

    @Transactional
    public void removeGame(Long gameId, GamingPlatform platform) {
        AppUser user = currentUserService.get();
        if (userGameRepository.deleteByUserIdAndGameIdAndPlatform(user.getId(), gameId, platform) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game is not in your library.");
        }
    }

    private LibraryGameResponse toResponse(UserGame ownership) {
        Game game = ownership.getGame();
        return new LibraryGameResponse(game.getId(), game.getTitle(), game.getTags(),
                game.isMultiplayerSupport(), game.getMaxPlayers(), ownership.getPlatform());
    }
}
