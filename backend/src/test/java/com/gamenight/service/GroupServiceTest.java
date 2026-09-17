package com.gamenight.service;

import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.UserGame;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMembershipRepository membershipRepository;
    @Mock private UserGameRepository userGameRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(currentUserService, groupRepository, membershipRepository, userGameRepository);
    }

    @Test
    void sharedGamesOnlyIncludesGamesOwnedByEveryMember() {
        AppUser currentUser = user(1L);
        AppUser secondUser = user(2L);
        GameGroup group = mock(GameGroup.class);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(currentUserService.get()).thenReturn(currentUser);
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
        GroupMembership firstMembership = membership(currentUser);
        GroupMembership secondMembership = membership(secondUser);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(firstMembership, secondMembership));

        Game terraria = game(100L);
        when(terraria.getTitle()).thenReturn("Terraria");
        Game portal = game(101L);
        UserGame firstTerraria = ownership(terraria);
        UserGame secondTerraria = ownership(terraria);
        UserGame firstPortal = ownership(portal);
        when(userGameRepository.findByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(
                firstTerraria, secondTerraria, firstPortal
        ));

        List<GameResponse> result = groupService.getSharedGames(10L);

        assertThat(result).containsExactly(new GameResponse(100L, "Terraria"));
    }

    @Test
    void oneMemberGetsTheirEntireLibrary() {
        AppUser currentUser = user(1L);
        allowGroupAccess(currentUser, 10L);
        GroupMembership membership = membership(currentUser);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(membership));

        Game terraria = game(100L);
        when(terraria.getTitle()).thenReturn("Terraria");
        Game minecraft = game(101L);
        when(minecraft.getTitle()).thenReturn("Minecraft");
        UserGame terrariaOwnership = ownership(terraria);
        UserGame minecraftOwnership = ownership(minecraft);
        when(userGameRepository.findByUserIdIn(List.of(1L)))
                .thenReturn(List.of(terrariaOwnership, minecraftOwnership));

        List<GameResponse> result = groupService.getSharedGames(10L);

        assertThat(result).containsExactly(
                new GameResponse(101L, "Minecraft"),
                new GameResponse(100L, "Terraria")
        );
    }

    @Test
    void noCommonOwnershipReturnsAnEmptyList() {
        AppUser currentUser = user(1L);
        AppUser secondUser = user(2L);
        allowGroupAccess(currentUser, 10L);
        GroupMembership firstMembership = membership(currentUser);
        GroupMembership secondMembership = membership(secondUser);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(firstMembership, secondMembership));

        UserGame firstOwnership = ownership(game(100L));
        UserGame secondOwnership = ownership(game(101L));
        when(userGameRepository.findByUserIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(firstOwnership, secondOwnership));

        assertThat(groupService.getSharedGames(10L)).isEmpty();
    }

    @Test
    void noMembersReturnsAnEmptyList() {
        AppUser currentUser = user(1L);
        allowGroupAccess(currentUser, 10L);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L)).thenReturn(List.of());

        assertThat(groupService.getSharedGames(10L)).isEmpty();
        verifyNoInteractions(userGameRepository);
    }

    @Test
    void nonMemberCannotViewSharedGames() {
        AppUser currentUser = user(1L);
        GameGroup group = mock(GameGroup.class);
        when(currentUserService.get()).thenReturn(currentUser);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getSharedGames(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You are not a member of this group.");
    }

    private void allowGroupAccess(AppUser currentUser, Long groupId) {
        GameGroup group = mock(GameGroup.class);
        when(currentUserService.get()).thenReturn(currentUser);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(currentUser.getId(), groupId)).thenReturn(true);
    }

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private GroupMembership membership(AppUser user) {
        GroupMembership membership = mock(GroupMembership.class);
        when(membership.getUser()).thenReturn(user);
        return membership;
    }

    private Game game(Long id) {
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(id);
        return game;
    }

    private UserGame ownership(Game game) {
        UserGame ownership = mock(UserGame.class);
        when(ownership.getGame()).thenReturn(game);
        return ownership;
    }
}
