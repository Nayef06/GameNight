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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
