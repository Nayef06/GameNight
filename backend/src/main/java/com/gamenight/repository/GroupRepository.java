package com.gamenight.repository;

import com.gamenight.model.GameGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<GameGroup, Long> {
    boolean existsByJoinCode(String joinCode);
    Optional<GameGroup> findByJoinCode(String joinCode);
}

