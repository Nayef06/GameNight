package com.gamenight.repository;

import com.gamenight.model.GameGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<GameGroup, Long> {
    boolean existsByJoinCode(String joinCode);
    Optional<GameGroup> findByJoinCode(String joinCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameGroup g where g.id = :groupId")
    Optional<GameGroup> findByIdForUpdate(@Param("groupId") Long groupId);
}

