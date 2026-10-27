package com.gamenight.controller;

import com.gamenight.dto.SessionDtos.CreateSessionRequest;
import com.gamenight.dto.SessionDtos.SessionResponse;
import com.gamenight.dto.SessionDtos.VoteRequest;
import com.gamenight.dto.SessionDtos.VotingResultsResponse;
import com.gamenight.service.GameNightSessionService;
import com.gamenight.service.SessionVotingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/sessions")
public class GameNightSessionController {
    private final GameNightSessionService sessionService;
    private final SessionVotingService votingService;

    public GameNightSessionController(GameNightSessionService sessionService,
                                      SessionVotingService votingService) {
        this.sessionService = sessionService;
        this.votingService = votingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse create(@PathVariable Long groupId,
                                  @Valid @RequestBody CreateSessionRequest request) {
        return sessionService.create(groupId, request.gameIds());
    }

    @GetMapping("/active")
    public ResponseEntity<SessionResponse> getActive(@PathVariable Long groupId) {
        return sessionService.getActive(groupId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{sessionId}")
    public SessionResponse get(@PathVariable Long groupId, @PathVariable Long sessionId) {
        return sessionService.get(groupId, sessionId);
    }

    @PostMapping("/{sessionId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vote(@PathVariable Long groupId, @PathVariable Long sessionId,
                     @Valid @RequestBody VoteRequest request) {
        votingService.vote(groupId, sessionId, request.gameId());
    }

    @GetMapping("/{sessionId}/results")
    public VotingResultsResponse results(@PathVariable Long groupId, @PathVariable Long sessionId) {
        return votingService.results(groupId, sessionId);
    }

    @PostMapping("/{sessionId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable Long groupId, @PathVariable Long sessionId) {
        votingService.close(groupId, sessionId);
    }
}
