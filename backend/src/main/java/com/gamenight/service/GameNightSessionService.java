package com.gamenight.service;

import com.gamenight.dto.GroupDtos.SharedGameResponse;
import com.gamenight.dto.SessionDtos.CandidateGame;
import com.gamenight.dto.SessionDtos.SessionResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import com.gamenight.repository.GameNightSessionRepository;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class GameNightSessionService {
    private final CurrentUserService currentUserService;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GameNightSessionRepository sessionRepository;
    private final GameRepository gameRepository;
    private final GroupService groupService;

    public GameNightSessionService(CurrentUserService currentUserService, GroupRepository groupRepository,
                                   GroupMembershipRepository membershipRepository,
                                   GameNightSessionRepository sessionRepository,
                                   GameRepository gameRepository, GroupService groupService) {
        this.currentUserService = currentUserService;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.sessionRepository = sessionRepository;
        this.gameRepository = gameRepository;
        this.groupService = groupService;
    }

    @Transactional
    public SessionResponse create(Long groupId, List<Long> gameIds) {
        AppUser user = currentUserService.get();
        // Serialize creation for this group so two requests cannot both see no active session.
        GameGroup group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
        requireMember(groupId, user);

        if (sessionRepository.findByGroupIdAndStatus(groupId, GameNightSessionStatus.OPEN).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This group already has an active GameNight session.");
        }
        if (gameIds == null || gameIds.size() < 2 || gameIds.size() > 5
                || gameIds.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select between 2 and 5 candidate games.");
        }
        if (new HashSet<>(gameIds).size() != gameIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate candidate games are not allowed.");
        }

        Set<Long> sharedGameIds = groupService.getSharedGames(groupId).stream()
                .map(SharedGameResponse::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!sharedGameIds.containsAll(gameIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Candidate games must be owned by every group member.");
        }
        Set<Game> candidates = new LinkedHashSet<>(gameRepository.findAllById(gameIds));
        if (candidates.size() != gameIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Candidate games must be owned by every group member.");
        }
        GameNightSession session = sessionRepository.save(new GameNightSession(group, user, candidates));
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActive(Long groupId) {
        requireGroupMember(groupId);
        return sessionRepository.findByGroupIdAndStatus(groupId, GameNightSessionStatus.OPEN)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionResponse get(Long groupId, Long sessionId) {
        requireGroupMember(groupId);
        GameNightSession session = sessionRepository.findByIdAndGroupId(sessionId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found."));
        return toResponse(session);
    }

    private void requireGroupMember(Long groupId) {
        AppUser user = currentUserService.get();
        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found.");
        }
        requireMember(groupId, user);
    }

    private void requireMember(Long groupId, AppUser user) {
        if (!membershipRepository.existsByUserIdAndGroupId(user.getId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group.");
        }
    }

    private SessionResponse toResponse(GameNightSession session) {
        List<CandidateGame> candidates = session.getCandidates().stream()
                .map(game -> new CandidateGame(game.getId(), game.getTitle()))
                .sorted(Comparator.comparing(CandidateGame::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new SessionResponse(session.getId(), session.getGroup().getId(),
                session.getCreatedBy().getUsername(), session.getCreatedAt(),
                session.getStatus().name(), candidates);
    }
}
