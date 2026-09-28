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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void userCanCreateAGroup() {
        AppUser currentUser = mock(AppUser.class);
        when(currentUserService.get()).thenReturn(currentUser);
        when(groupRepository.existsByJoinCode(any())).thenReturn(false);
        when(groupRepository.save(any(GameGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = groupService.create(" Friday Night Group ");

        assertThat(result.name()).isEqualTo("Friday Night Group");
        assertThat(result.joinCode()).hasSize(6);
        verify(membershipRepository).save(any(GroupMembership.class));
    }

    @Test
    void anotherUserCanJoinUsingTheJoinCode() {
        AppUser alex = user(2L);
        GameGroup group = mock(GameGroup.class);
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Friday Night Group");
        when(group.getJoinCode()).thenReturn("ABC234");
        when(currentUserService.get()).thenReturn(alex);
        when(groupRepository.findByJoinCode("ABC234")).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L)).thenReturn(false);

        var result = groupService.join(" abc234 ");

        assertThat(result.name()).isEqualTo("Friday Night Group");
        assertThat(result.joinCode()).isEqualTo("ABC234");
        verify(membershipRepository).save(any(GroupMembership.class));
    }

    @Test
    void groupCreatorCanRemoveAnotherMember() {
        AppUser creator = user(1L);
        GameGroup group = groupOwnedBy(creator);
        when(currentUserService.get()).thenReturn(creator);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L)).thenReturn(true);

        groupService.removeMember(10L, 2L);

        verify(membershipRepository).deleteByUserIdAndGroupId(2L, 10L);
    }

    @Test
    void normalMemberCannotRemoveAnotherMember() {
        AppUser creator = user(1L);
        AppUser normalMember = user(2L);
        GameGroup group = groupOwnedBy(creator);
        when(currentUserService.get()).thenReturn(normalMember);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.removeMember(10L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only the group creator can remove members.");
        verify(membershipRepository, never()).deleteByUserIdAndGroupId(any(), any());
    }

    @Test
    void removedMemberNoLongerBelongsToTheGroup() {
        AppUser creator = user(1L);
        GameGroup group = groupOwnedBy(creator);
        AtomicBoolean removed = new AtomicBoolean(false);
        when(currentUserService.get()).thenReturn(creator);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L))
                .thenAnswer(invocation -> !removed.get());
        when(membershipRepository.deleteByUserIdAndGroupId(2L, 10L)).thenAnswer(invocation -> {
            removed.set(true);
            return 1L;
        });

        groupService.removeMember(10L, 2L);

        assertThat(membershipRepository.existsByUserIdAndGroupId(2L, 10L)).isFalse();
    }

    @Test
    void normalMemberCanLeaveAGroup() {
        AppUser creator = user(1L);
        AppUser normalMember = user(2L);
        GameGroup group = groupOwnedBy(creator);
        when(currentUserService.get()).thenReturn(normalMember);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L)).thenReturn(true);

        groupService.leave(10L);

        verify(membershipRepository).deleteByUserIdAndGroupId(2L, 10L);
    }

    @Test
    void groupCreatorCannotLeaveTheirOwnGroup() {
        AppUser creator = user(1L);
        GameGroup group = groupOwnedBy(creator);
        when(currentUserService.get()).thenReturn(creator);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.leave(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("The group creator cannot leave the group.");
        verify(membershipRepository, never()).deleteByUserIdAndGroupId(any(), any());
    }

    @Test
    void sharedGamesAreRecomputedAfterMembershipChanges() {
        AppUser creator = user(1L);
        AppUser secondUser = user(2L);
        GameGroup group = groupOwnedBy(creator);
        AtomicBoolean removed = new AtomicBoolean(false);
        when(currentUserService.get()).thenReturn(creator);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(membershipRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
        when(membershipRepository.existsByUserIdAndGroupId(2L, 10L))
                .thenAnswer(invocation -> !removed.get());
        when(membershipRepository.deleteByUserIdAndGroupId(2L, 10L)).thenAnswer(invocation -> {
            removed.set(true);
            return 1L;
        });

        GroupMembership creatorMembership = membership(creator);
        GroupMembership secondMembership = membership(secondUser);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenAnswer(invocation -> removed.get()
                        ? List.of(creatorMembership)
                        : List.of(creatorMembership, secondMembership));

        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game minecraft = game(101L, "Minecraft", "Survival", true, 8);
        UserGame creatorTerraria = ownership(terraria);
        UserGame secondTerraria = ownership(terraria);
        UserGame creatorMinecraft = ownership(minecraft);
        when(userGameRepository.findByUserIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(creatorTerraria, secondTerraria, creatorMinecraft));
        when(userGameRepository.findByUserIdIn(List.of(1L)))
                .thenReturn(List.of(creatorTerraria, creatorMinecraft));

        assertThat(groupService.getSharedGames(10L)).extracting(GameResponse::title)
                .containsExactly("Terraria");

        groupService.removeMember(10L, 2L);

        assertThat(groupService.getSharedGames(10L)).extracting(GameResponse::title)
                .containsExactly("Minecraft", "Terraria");
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

        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game portal = game(101L);
        UserGame firstTerraria = ownership(terraria);
        UserGame secondTerraria = ownership(terraria);
        UserGame firstPortal = ownership(portal);
        when(userGameRepository.findByUserIdIn(List.of(1L, 2L))).thenReturn(List.of(
                firstTerraria, secondTerraria, firstPortal
        ));

        List<GameResponse> result = groupService.getSharedGames(10L);

        assertThat(result).containsExactly(
                new GameResponse(100L, "Terraria", "Survival", true, 8));
    }

    @Test
    void oneMemberGetsTheirEntireLibrary() {
        AppUser currentUser = user(1L);
        allowGroupAccess(currentUser, 10L);
        GroupMembership membership = membership(currentUser);
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(10L))
                .thenReturn(List.of(membership));

        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game minecraft = game(101L, "Minecraft", "Survival", true, 8);
        UserGame terrariaOwnership = ownership(terraria);
        UserGame minecraftOwnership = ownership(minecraft);
        when(userGameRepository.findByUserIdIn(List.of(1L)))
                .thenReturn(List.of(terrariaOwnership, minecraftOwnership));

        List<GameResponse> result = groupService.getSharedGames(10L);

        assertThat(result).containsExactly(
                new GameResponse(101L, "Minecraft", "Survival", true, 8),
                new GameResponse(100L, "Terraria", "Survival", true, 8)
        );
    }

    @Test
    void sharedGamesCanBeFilteredByGenre() {
        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game portal = game(101L, "Portal", "Puzzle", false, 1);
        prepareOneMemberGames(10L, terraria, portal);

        assertThat(groupService.getSharedGames(10L, "survival", null, null))
                .containsExactly(new GameResponse(100L, "Terraria", "Survival", true, 8));
    }

    @Test
    void sharedGamesCanBeFilteredByMultiplayerSupport() {
        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game portal = game(101L, "Portal", "Puzzle", false, 1);
        prepareOneMemberGames(10L, terraria, portal);

        assertThat(groupService.getSharedGames(10L, null, false, null))
                .containsExactly(new GameResponse(101L, "Portal", "Puzzle", false, 1));
    }

    @Test
    void sharedGamesCanBeFilteredByRequiredPlayerCount() {
        Game terraria = game(100L, "Terraria", "Survival", true, 8);
        Game portal = game(101L, "Portal", "Puzzle", true, 2);
        prepareOneMemberGames(10L, terraria, portal);

        assertThat(groupService.getSharedGames(10L, null, null, 4))
                .containsExactly(new GameResponse(100L, "Terraria", "Survival", true, 8));
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

    private void prepareOneMemberGames(Long groupId, Game... games) {
        AppUser currentUser = user(1L);
        allowGroupAccess(currentUser, groupId);
        GroupMembership groupMembership = membership(currentUser);
        List<UserGame> ownerships = java.util.Arrays.stream(games).map(this::ownership).toList();
        when(membershipRepository.findByGroupIdOrderByUserUsernameAsc(groupId))
                .thenReturn(List.of(groupMembership));
        when(userGameRepository.findByUserIdIn(List.of(1L)))
                .thenReturn(ownerships);
    }

    private AppUser user(Long id) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private GameGroup groupOwnedBy(AppUser creator) {
        GameGroup group = mock(GameGroup.class);
        when(group.getCreatedBy()).thenReturn(creator);
        return group;
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

    private Game game(Long id, String title, String genre, boolean multiplayerSupport, int maxPlayers) {
        Game game = game(id);
        lenient().when(game.getTitle()).thenReturn(title);
        lenient().when(game.getGenre()).thenReturn(genre);
        lenient().when(game.isMultiplayerSupport()).thenReturn(multiplayerSupport);
        lenient().when(game.getMaxPlayers()).thenReturn(maxPlayers);
        return game;
    }

    private UserGame ownership(Game game) {
        UserGame ownership = mock(UserGame.class);
        when(ownership.getGame()).thenReturn(game);
        return ownership;
    }
}
