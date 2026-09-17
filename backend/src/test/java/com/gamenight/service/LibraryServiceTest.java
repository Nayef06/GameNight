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
        Game game = game(10L, "Lethal Company");
        when(currentUserService.get()).thenReturn(user);
        when(gameRepository.findByNormalizedTitle("lethal company")).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenReturn(game);
        when(userGameRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(false);

        GameResponse response = libraryService.addGame("  Lethal   Company ");

        assertThat(response).isEqualTo(new GameResponse(10L, "Lethal Company"));
        verify(userGameRepository).save(any(UserGame.class));
    }

    @Test
    void duplicateLibraryEntriesAreRejected() {
        AppUser user = user(1L);
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(10L);
        when(currentUserService.get()).thenReturn(user);
        when(gameRepository.findByNormalizedTitle("terraria")).thenReturn(Optional.of(game));
        when(userGameRepository.existsByUserIdAndGameId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> libraryService.addGame("TERRARIA"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Game is already in your library.");
        verify(userGameRepository, never()).save(any());
    }

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Game game(Long id, String title) {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(id);
        when(game.getTitle()).thenReturn(title);
        return game;
    }
}
