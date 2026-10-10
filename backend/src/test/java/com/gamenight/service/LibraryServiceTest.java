package com.gamenight.service;

import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.UserGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GameRepository gameRepository;
    @Mock private UserGameRepository userGameRepository;

    private LibraryService libraryService;

    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(currentUserService, gameRepository, userGameRepository);
    }

    @Test
    void userCanAddAGame() {
        AppUser user = user(1L);
        Game game = game(10L, "Lethal Company", List.of("Co-op", "Horror"), true, 4);
        when(currentUserService.get()).thenReturn(user);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(userGameRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(false);

        GameResponse response = libraryService.addGame(10L);

        assertThat(response).isEqualTo(new GameResponse(
                10L, "Lethal Company", List.of("Co-op", "Horror"), true, 4));
        verify(gameRepository, never()).save(any());
        verify(userGameRepository).save(any(UserGame.class));
    }

    @Test
    void gamesOutsideTheCatalogAreRejected() {
        AppUser user = user(1L);
        Game game = game(10L, "Unknown Game", List.of("Action"), true, 4);
        when(currentUserService.get()).thenReturn(user);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> libraryService.addGame(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Choose a game from the catalog.");
        verify(gameRepository, never()).save(any());
        verify(userGameRepository, never()).save(any());
    }

    @Test
    void duplicateLibraryEntriesAreRejected() {
        AppUser user = user(1L);
        Game game = game(10L, "Terraria", List.of("Survival"), true, 8);
        when(currentUserService.get()).thenReturn(user);
        when(gameRepository.findById(10L)).thenReturn(Optional.of(game));
        when(userGameRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> libraryService.addGame(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Game is already in your library.");
        verify(userGameRepository, never()).save(any());
    }

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        org.mockito.Mockito.lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Game game(Long id, String title, List<String> tags, boolean multiplayerSupport, int maxPlayers) {
        Game game = mock(Game.class);
        org.mockito.Mockito.lenient().when(game.getId()).thenReturn(id);
        when(game.getNormalizedTitle()).thenReturn(title.toLowerCase(java.util.Locale.ROOT));
        org.mockito.Mockito.lenient().when(game.getTitle()).thenReturn(title);
        org.mockito.Mockito.lenient().when(game.getTags()).thenReturn(tags);
        org.mockito.Mockito.lenient().when(game.isMultiplayerSupport()).thenReturn(multiplayerSupport);
        org.mockito.Mockito.lenient().when(game.getMaxPlayers()).thenReturn(maxPlayers);
        return game;
    }
}
