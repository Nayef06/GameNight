package com.gamenight.service;

import com.gamenight.dto.GroupDtos.GameSelectionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GameSelectionService {
    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GameRepository gameRepository;
    private final UserGameRepository userGameRepository;
    private final GroupGameSelectionRepository selectionRepository;

    public GameSelectionService(CurrentUserService currentUserService, GroupRepository groupRepository,
                                GroupMembershipRepository membershipRepository, GameRepository gameRepository,
                                UserGameRepository userGameRepository,
                                GroupGameSelectionRepository selectionRepository) {
        this.currentUserService = currentUserService;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.gameRepository = gameRepository;
        this.userGameRepository = userGameRepository;
        this.selectionRepository = selectionRepository;
    }

    @Transactional
    public GameSelectionResponse select(Long groupId, Long gameId) {
        AppUser user = currentUserService.get();
        GameGroup group = requireGroupMember(groupId, user);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found."));

        List<Long> memberIds = membershipRepository.findByGroupIdOrderByUserUsernameAsc(groupId).stream()
                .map(GroupMembership::getUser)
                .map(AppUser::getId)
                .toList();
        if (userGameRepository.countByGameIdAndUserIdIn(gameId, memberIds) != memberIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This game is not owned by every group member.");
        }

        return toResponse(selectionRepository.save(new GroupGameSelection(group, game)));
    }

    @Transactional(readOnly = true)
    public List<GameSelectionResponse> getRecent(Long groupId) {
        AppUser user = currentUserService.get();
        requireGroupMember(groupId, user);
        return selectionRepository.findTop5ByGroupIdOrderBySelectedAtDescIdDesc(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    private GameGroup requireGroupMember(Long groupId, AppUser user) {
        GameGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
        if (!membershipRepository.existsByUserIdAndGroupId(user.getId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group.");
        }
        return group;
    }

    private GameSelectionResponse toResponse(GroupGameSelection selection) {
        return new GameSelectionResponse(selection.getId(), selection.getGame().getId(),
                selection.getGame().getTitle(), selection.getSelectedAt());
    }
}
