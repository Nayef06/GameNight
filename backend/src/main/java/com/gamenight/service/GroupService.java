package com.gamenight.service;

import com.gamenight.dto.GroupDtos.GroupDetails;
import com.gamenight.dto.GroupDtos.GroupMember;
import com.gamenight.dto.GroupDtos.GroupSummary;
import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import com.gamenight.repository.UserGameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GroupService {
    private static final String CODE_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserGameRepository userGameRepository;
    private final SecureRandom random = new SecureRandom();

    public GroupService(CurrentUserService currentUserService, GroupRepository groupRepository,
                        GroupMembershipRepository membershipRepository, UserGameRepository userGameRepository) {
        this.currentUserService = currentUserService;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userGameRepository = userGameRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> getMyGroups() {
        AppUser user = currentUserService.get();
        return membershipRepository.findByUserIdOrderByGroupNameAsc(user.getId()).stream()
                .map(membership -> toSummary(membership.getGroup()))
                .toList();
    }

    @Transactional
    public GroupSummary create(String requestedName) {
        AppUser user = currentUserService.get();
        GameGroup group = groupRepository.save(new GameGroup(requestedName.trim(), generateJoinCode(), user));
        membershipRepository.save(new GroupMembership(user, group));
        return toSummary(group);
    }

    @Transactional
    public GroupSummary join(String requestedCode) {
        AppUser user = currentUserService.get();
        String code = requestedCode.trim().toUpperCase(Locale.ROOT);
        GameGroup group = groupRepository.findByJoinCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code."));
        if (membershipRepository.existsByUserIdAndGroupId(user.getId(), group.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group.");
        }
        membershipRepository.save(new GroupMembership(user, group));
        return toSummary(group);
    }

    @Transactional(readOnly = true)
    public GroupDetails getDetails(Long groupId) {
        AppUser user = currentUserService.get();
        GameGroup group = requireGroupMember(groupId, user);
        Long creatorId = group.getCreatedBy().getId();
        List<GroupMember> members = membershipRepository.findByGroupIdOrderByUserUsernameAsc(groupId).stream()
                .map(membership -> new GroupMember(
                        membership.getUser().getId(),
                        membership.getUser().getUsername(),
                        Objects.equals(membership.getUser().getId(), creatorId)))
                .toList();
        return new GroupDetails(group.getId(), group.getName(), group.getJoinCode(),
                group.getCreatedBy().getUsername(), group.getCreatedAt(),
                Objects.equals(user.getId(), creatorId), members);
    }

    @Transactional
    public void removeMember(Long groupId, Long memberUserId) {
        AppUser user = currentUserService.get();
        GameGroup group = requireGroupMember(groupId, user);
        Long creatorId = group.getCreatedBy().getId();

        if (!Objects.equals(user.getId(), creatorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the group creator can remove members.");
        }
        if (Objects.equals(memberUserId, creatorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The group creator cannot be removed.");
        }
        if (!membershipRepository.existsByUserIdAndGroupId(memberUserId, groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Member not found in this group.");
        }

        membershipRepository.deleteByUserIdAndGroupId(memberUserId, groupId);
    }

    @Transactional
    public void leave(Long groupId) {
        AppUser user = currentUserService.get();
        GameGroup group = requireGroupMember(groupId, user);
        if (Objects.equals(user.getId(), group.getCreatedBy().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The group creator cannot leave the group.");
        }
        membershipRepository.deleteByUserIdAndGroupId(user.getId(), groupId);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> getSharedGames(Long groupId) {
        return getSharedGames(groupId, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<GameResponse> getSharedGames(Long groupId, String requestedGenre,
                                             Boolean multiplayerSupport, Integer minPlayers) {
        if (minPlayers != null && minPlayers < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Minimum player capacity must be at least 1.");
        }

        AppUser user = currentUserService.get();
        requireGroupMember(groupId, user);
        List<Long> memberIds = membershipRepository.findByGroupIdOrderByUserUsernameAsc(groupId).stream()
                .map(membership -> membership.getUser().getId())
                .toList();
        if (memberIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> ownerCounts = new HashMap<>();
        Map<Long, Game> games = new HashMap<>();
        for (UserGame userGame : userGameRepository.findByUserIdIn(memberIds)) {
            Game game = userGame.getGame();
            ownerCounts.merge(game.getId(), 1, Integer::sum);
            games.put(game.getId(), game);
        }

        String genre = requestedGenre == null ? null : requestedGenre.trim();
        return games.values().stream()
                .filter(game -> ownerCounts.get(game.getId()) == memberIds.size())
                .filter(game -> genre == null || genre.isBlank() || genre.equalsIgnoreCase(game.getGenre()))
                .filter(game -> multiplayerSupport == null
                        || multiplayerSupport == game.isMultiplayerSupport())
                .filter(game -> minPlayers == null || game.getMaxPlayers() >= minPlayers)
                .sorted(Comparator.comparing(Game::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(game -> new GameResponse(game.getId(), game.getTitle(), game.getGenre(),
                        game.isMultiplayerSupport(), game.getMaxPlayers()))
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

    private String generateJoinCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_CHARACTERS.charAt(random.nextInt(CODE_CHARACTERS.length())));
            }
            if (!groupRepository.existsByJoinCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique join code");
    }

    private GroupSummary toSummary(GameGroup group) {
        return new GroupSummary(group.getId(), group.getName(), group.getJoinCode(),
                membershipRepository.countByGroupId(group.getId()));
    }
}
