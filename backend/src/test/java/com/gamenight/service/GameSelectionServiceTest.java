package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupGameSelection;
import com.gamenight.model.GroupMembership;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.GroupGameSelectionRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import com.gamenight.repository.UserGameRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSelectionServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMembershipRepository membershipRepository;
    @Mock private GameRepository gameRepository;
    @Mock private UserGameRepository userGameRepository;
    @Mock private GroupGameSelectionRepository selectionRepository;

    private GameSelectionService selectionService;

    @BeforeEach
    void setUp() {
        selectionService = new GameSelectionService(currentUserService, groupRepository,
                membershipRepository, gameRepository, userGameRepository, selectionRepository);
    }

    @Test
    void groupMemberCanRecordAGameOwnedByEveryMember() {
        AppUser currentUser = user(1L);
        AppUser secondMember = user(2L);
        GameGroup group = group(10L);
        Game game = game(20L, "Terraria");
        GroupMembership currentMembership = membership(currentUser);
        GroupMembership secondMembership = membership(secondMember);
        allowGroupAccess(currentUser, group);
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(currentMembership, secondMembership));
        when(userGameRepository.countByGameIdAndUserIdIn(20L, List.of(1L, 2L))).thenReturn(2L);
        when(selectionRepository.save(any(GroupGameSelection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = selectionService.select(10L, 20L);

        assertThat(result.gameId()).isEqualTo(20L);
        assertThat(result.gameTitle()).isEqualTo("Terraria");
        assertThat(result.selectedAt()).isNotNull();
        verify(selectionRepository).save(any(GroupGameSelection.class));
    }

    @Test
    void nonMemberCannotRecordASelection() {
        AppUser currentUser = user(1L);
        GameGroup group = mock(GameGroup.class);
        when(currentUserService.get()).thenReturn(currentUser);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> selectionService.select(10L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You are not a member of this group.");
        verify(selectionRepository, never()).save(any());
    }

    @Test
    void gameNotOwnedByEveryMemberCannotBeSelected() {
        AppUser currentUser = user(1L);
        AppUser secondMember = user(2L);
        GameGroup group = group(10L);
        Game game = mock(Game.class);
        GroupMembership currentMembership = membership(currentUser);
        GroupMembership secondMembership = membership(secondMember);
        allowGroupAccess(currentUser, group);
        when(gameRepository.findById(20L)).thenReturn(Optional.of(game));
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(currentMembership, secondMembership));
        when(userGameRepository.countByGameIdAndUserIdIn(20L, List.of(1L, 2L))).thenReturn(1L);

        assertThatThrownBy(() -> selectionService.select(10L, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not owned by every group member");
        verify(selectionRepository, never()).save(any());
    }

    private void allowGroupAccess(AppUser currentUser, GameGroup group) {
        when(currentUserService.get()).thenReturn(currentUser);
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(currentUser.getId(), group.getId()))
                .thenReturn(true);
    }

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private GameGroup group(Long id) {
        GameGroup group = mock(GameGroup.class);
        when(group.getId()).thenReturn(id);
        return group;
    }

    private Game game(Long id, String title) {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(id);
        when(game.getTitle()).thenReturn(title);
        return game;
    }

    private GroupMembership membership(AppUser user) {
        GroupMembership membership = mock(GroupMembership.class);
        when(membership.getUser()).thenReturn(user);
        return membership;
    }
}
