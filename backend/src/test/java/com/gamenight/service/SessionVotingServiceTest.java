package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import com.gamenight.model.SessionVote;
import com.gamenight.repository.GameNightSessionRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.SessionVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionVotingServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GroupMembershipRepository membershipRepository;
    @Mock private GameNightSessionRepository sessionRepository;
    @Mock private SessionVoteRepository voteRepository;

    private SessionVotingService service;
    private AppUser user;
    private GameGroup group;
    private Game terraria;
    private Game minecraft;
    private GameNightSession session;

    @BeforeEach
    void setUp() {
        service = new SessionVotingService(currentUserService, membershipRepository,
                sessionRepository, voteRepository);
        user = mock(AppUser.class);
        group = mock(GameGroup.class);
        terraria = game(1L, "Terraria");
        minecraft = game(2L, "Minecraft");
        session = new GameNightSession(group, user, Set.of(terraria, minecraft));
        lenient().when(user.getId()).thenReturn(5L);
        lenient().when(currentUserService.get()).thenReturn(user);
        lenient().when(membershipRepository.existsByUserIdAndGroupId(5L, 10L)).thenReturn(true);
        lenient().when(sessionRepository.findByIdAndGroupIdForUpdate(20L, 10L))
                .thenReturn(Optional.of(session));
        lenient().when(sessionRepository.findByIdAndGroupId(20L, 10L))
                .thenReturn(Optional.of(session));
    }

    @Test
    void groupMemberCanVote() {
        service.vote(10L, 20L, 1L);

        ArgumentCaptor<SessionVote> saved = ArgumentCaptor.forClass(SessionVote.class);
        verify(voteRepository).save(saved.capture());
        assertThat(saved.getValue().getSession()).isSameAs(session);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getGame()).isSameAs(terraria);
    }

    @Test
    void nonMemberCannotVoteOrViewResults() {
        when(membershipRepository.existsByUserIdAndGroupId(5L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.vote(10L, 20L, 1L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403 FORBIDDEN");
        assertThatThrownBy(() -> service.results(10L, 20L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403 FORBIDDEN");
        verify(voteRepository, never()).save(any());
    }

    @Test
    void cannotVoteForNonCandidate() {
        assertThatThrownBy(() -> service.vote(10L, 20L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("session candidate game");
        verify(voteRepository, never()).save(any());
    }

    @Test
    void changingVoteUpdatesExistingRecord() {
        SessionVote existing = new SessionVote(session, user, terraria);
        when(voteRepository.findBySessionIdAndUserId(20L, 5L)).thenReturn(Optional.of(existing));

        service.vote(10L, 20L, 2L);

        assertThat(existing.getGame()).isSameAs(minecraft);
        verify(voteRepository, never()).save(any());
    }

    @Test
    void resultsCountVotesAndIncludeZeroVoteCandidates() {
        AppUser other = mock(AppUser.class);
        when(voteRepository.findBySessionId(20L)).thenReturn(List.of(
                new SessionVote(session, user, terraria),
                new SessionVote(session, other, terraria)));

        var results = service.results(10L, 20L);

        assertThat(results.currentUserVoteGameId()).isEqualTo(1L);
        assertThat(results.results()).extracting(result -> result.title() + ":" + result.votes())
                .containsExactly("Terraria:2", "Minecraft:0");
    }

    @Test
    void groupCreatorCanCloseSession() {
        when(group.getCreatedBy()).thenReturn(user);

        service.close(10L, 20L);

        assertThat(session.getStatus()).isEqualTo(GameNightSessionStatus.CLOSED);
    }

    @Test
    void normalMemberCannotCloseSession() {
        AppUser creator = mock(AppUser.class);
        when(creator.getId()).thenReturn(9L);
        when(group.getCreatedBy()).thenReturn(creator);

        assertThatThrownBy(() -> service.close(10L, 20L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403 FORBIDDEN");
        assertThat(session.getStatus()).isEqualTo(GameNightSessionStatus.OPEN);
    }

    @Test
    void votesAreRejectedAfterClosing() {
        session.close();

        assertThatThrownBy(() -> service.vote(10L, 20L, 1L))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Voting is closed");
        verify(voteRepository, never()).save(any());
    }

    @Test
    void tiedResultsAreSortedAlphabetically() {
        AppUser other = mock(AppUser.class);
        when(voteRepository.findBySessionId(20L)).thenReturn(List.of(
                new SessionVote(session, user, terraria),
                new SessionVote(session, other, minecraft)));

        var results = service.results(10L, 20L);

        assertThat(results.results()).extracting(result -> result.title() + ":" + result.votes())
                .containsExactly("Minecraft:1", "Terraria:1");
    }

    private Game game(Long id, String title) {
        Game game = mock(Game.class);
        lenient().when(game.getId()).thenReturn(id);
        lenient().when(game.getTitle()).thenReturn(title);
        return game;
    }
}
