package com.gamenight.service;

import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.UserGameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

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
    public List<GameResponse> getLibrary() {
        AppUser user = currentUserService.get();
        return userGameRepository.findByUserIdOrderByGameTitleAsc(user.getId()).stream()
                .map(userGame -> toResponse(userGame.getGame()))
                .toList();
    }

    @Transactional
    public GameResponse addGame(String requestedTitle, String requestedGenre,
                                boolean multiplayerSupport, int maxPlayers) {
        if (maxPlayers < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum players must be at least 1.");
        }
        String genre = requestedGenre.trim().replaceAll("\\s+", " ");
        if (genre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Genre cannot be blank.");
        }

        AppUser user = currentUserService.get();
        String title = requestedTitle.trim().replaceAll("\\s+", " ");
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        Game game = gameRepository.findByNormalizedTitle(normalizedTitle)
                .orElseGet(() -> gameRepository.save(
                        new Game(title, normalizedTitle, genre, multiplayerSupport, maxPlayers)));

        if (userGameRepository.existsByUserIdAndGameId(user.getId(), game.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already in your library.");
        }
        userGameRepository.save(new UserGame(user, game));
        return toResponse(game);
    }

    @Transactional
    public void removeGame(Long gameId) {
        AppUser user = currentUserService.get();
        if (userGameRepository.deleteByUserIdAndGameId(user.getId(), gameId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game is not in your library.");
        }
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(game.getId(), game.getTitle(), game.getGenre(),
                game.isMultiplayerSupport(), game.getMaxPlayers());
    }
}
