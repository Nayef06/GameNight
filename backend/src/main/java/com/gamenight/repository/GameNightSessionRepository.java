package com.gamenight.repository;

import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameNightSessionRepository extends JpaRepository<GameNightSession, Long> {
    Optional<GameNightSession> findByGroupIdAndStatus(Long groupId, GameNightSessionStatus status);
    Optional<GameNightSession> findByIdAndGroupId(Long id, Long groupId);
}
