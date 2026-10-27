package com.gamenight.service;

import com.gamenight.dto.GroupDtos.SharedGameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import com.gamenight.repository.GameNightSessionRepository;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameNightSessionServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMembershipRepository membershipRepository;
    @Mock private GameNightSessionRepository sessionRepository;
    @Mock private GameRepository gameRepository;
    @Mock private GroupService groupService;

    private GameNightSessionService service;
    private AppUser user;
    private GameGroup group;

    @BeforeEach
    void setUp() {
        service = new GameNightSessionService(currentUserService, groupRepository,
                membershipRepository, sessionRepository, gameRepository, groupService);
        user = mock(AppUser.class);
        group = mock(GameGroup.class);
        lenient().when(user.getId()).thenReturn(1L);
        lenient().when(user.getUsername()).thenReturn("alex");
        lenient().when(group.getId()).thenReturn(10L);
        lenient().when(currentUserService.get()).thenReturn(user);
        lenient().when(groupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        lenient().when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
    }

    @Test
    void memberCanCreateSessionWithSharedGames() {
        allowSharedGames(1L, 2L);
        Game first = game(1L, "Terraria");
        Game second = game(2L, "Minecraft");
        when(gameRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(sessionRepository.save(any(GameNightSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(10L, List.of(1L, 2L));

        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.groupId()).isEqualTo(10L);
        assertThat(response.createdBy()).isEqualTo("alex");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.candidates()).extracting(candidate -> candidate.title())
                .containsExactly("Minecraft", "Terraria");
        verify(sessionRepository).save(any(GameNightSession.class));
    }

    @Test
    void nonMemberCannotCreateOrViewSession() {
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(false);
        when(groupRepository.existsById(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, List.of(1L, 2L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> service.getActive(10L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403 FORBIDDEN");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void requiresTwoToFiveCandidates() {
        for (List<Long> ids : List.of(List.<Long>of(), List.of(1L),
                List.of(1L, 2L, 3L, 4L, 5L, 6L))) {
            assertThatThrownBy(() -> service.create(10L, ids))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Select between 2 and 5");
        }
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void rejectsGamesNotSharedByEveryMember() {
        allowSharedGames(1L);

        assertThatThrownBy(() -> service.create(10L, List.of(1L, 2L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("owned by every group member");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateCandidates() {
        assertThatThrownBy(() -> service.create(10L, List.of(1L, 1L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Duplicate candidate games");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void rejectsSecondOpenSession() {
        when(sessionRepository.findByGroupIdAndStatus(10L, GameNightSessionStatus.OPEN))
                .thenReturn(Optional.of(mock(GameNightSession.class)));

        assertThatThrownBy(() -> service.create(10L, List.of(1L, 2L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already has an active GameNight session");
        verify(sessionRepository, never()).save(any());
    }

    private void allowSharedGames(Long... ids) {
        when(groupService.getSharedGames(10L)).thenReturn(java.util.Arrays.stream(ids)
                .map(id -> new SharedGameResponse(id, "Game " + id, List.of(), true, 4, 0))
                .toList());
    }

    private Game game(Long id, String title) {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(id);
        when(game.getTitle()).thenReturn(title);
        return game;
    }
}
