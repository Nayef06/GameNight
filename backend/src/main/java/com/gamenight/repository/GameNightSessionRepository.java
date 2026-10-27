package com.gamenight.repository;

import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameNightSessionRepository extends JpaRepository<GameNightSession, Long> {
    Optional<GameNightSession> findByGroupIdAndStatus(Long groupId, GameNightSessionStatus status);
    Optional<GameNightSession> findByIdAndGroupId(Long id, Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameNightSession s where s.id = :sessionId and s.group.id = :groupId")
    Optional<GameNightSession> findByIdAndGroupIdForUpdate(@Param("sessionId") Long sessionId,
                                                            @Param("groupId") Long groupId);
}
