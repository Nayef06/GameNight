package com.gamenight.service;

import com.gamenight.dto.SessionDtos.VoteCount;
import com.gamenight.dto.SessionDtos.VotingResultsResponse;
import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import com.gamenight.model.SessionVote;
import com.gamenight.repository.GameNightSessionRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.SessionVoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SessionVotingService {
    private final CurrentUserService currentUserService;
    private final GroupMembershipRepository membershipRepository;
    private final GameNightSessionRepository sessionRepository;
    private final SessionVoteRepository voteRepository;

    public SessionVotingService(CurrentUserService currentUserService,
                                GroupMembershipRepository membershipRepository,
                                GameNightSessionRepository sessionRepository,
                                SessionVoteRepository voteRepository) {
        this.currentUserService = currentUserService;
        this.membershipRepository = membershipRepository;
        this.sessionRepository = sessionRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public void vote(Long groupId, Long sessionId, Long gameId) {
        AppUser user = currentUserService.get();
        // The same row lock is used by close, so a vote cannot commit after closing.
        GameNightSession session = requireSession(groupId, sessionId, user, true);
        if (session.getStatus() != GameNightSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting is closed.");
        }
        Game game = session.getCandidates().stream()
                .filter(candidate -> Objects.equals(candidate.getId(), gameId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Vote for a session candidate game."));

        SessionVote vote = voteRepository.findBySessionIdAndUserId(sessionId, user.getId())
                .orElse(null);
        if (vote == null) {
            voteRepository.save(new SessionVote(session, user, game));
        } else {
            vote.changeGame(game);
        }
    }

    @Transactional(readOnly = true)
    public VotingResultsResponse results(Long groupId, Long sessionId) {
        AppUser user = currentUserService.get();
        GameNightSession session = requireSession(groupId, sessionId, user, false);
        Map<Long, Long> counts = new HashMap<>();
        Long currentUserVoteGameId = null;
        for (SessionVote vote : voteRepository.findBySessionId(sessionId)) {
            Long votedGameId = vote.getGame().getId();
            counts.merge(votedGameId, 1L, Long::sum);
            if (Objects.equals(vote.getUser().getId(), user.getId())) {
                currentUserVoteGameId = votedGameId;
            }
        }
        List<VoteCount> results = session.getCandidates().stream()
                .map(game -> new VoteCount(game.getId(), game.getTitle(),
                        counts.getOrDefault(game.getId(), 0L)))
                .sorted(Comparator.comparingLong(VoteCount::votes).reversed()
                        .thenComparing(VoteCount::title, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(VoteCount::title)
                        .thenComparing(VoteCount::gameId))
                .toList();
        return new VotingResultsResponse(session.getStatus().name(), currentUserVoteGameId, results);
    }

    @Transactional
    public void close(Long groupId, Long sessionId) {
        AppUser user = currentUserService.get();
        GameNightSession session = requireSession(groupId, sessionId, user, true);
        if (!Objects.equals(session.getGroup().getCreatedBy().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the group creator can close voting.");
        }
        if (session.getStatus() != GameNightSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Voting is already closed.");
        }
        session.close();
    }

    private GameNightSession requireSession(Long groupId, Long sessionId, AppUser user,
                                            boolean forUpdate) {
        GameNightSession session = (forUpdate
                ? sessionRepository.findByIdAndGroupIdForUpdate(sessionId, groupId)
                : sessionRepository.findByIdAndGroupId(sessionId, groupId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found."));
        if (!membershipRepository.existsByUserIdAndGroupId(user.getId(), groupId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group.");
        }
        return session;
    }
}
