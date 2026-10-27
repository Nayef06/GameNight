package com.gamenight.repository;

import com.gamenight.model.SessionVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionVoteRepository extends JpaRepository<SessionVote, Long> {
    Optional<SessionVote> findBySessionIdAndUserId(Long sessionId, Long userId);
    List<SessionVote> findBySessionId(Long sessionId);
}
