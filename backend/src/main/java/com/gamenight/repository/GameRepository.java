package com.gamenight.repository;

import com.gamenight.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByNormalizedTitle(String normalizedTitle);
}

