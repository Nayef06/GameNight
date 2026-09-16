package com.gamenight.repository;

import com.gamenight.model.Game;
import com.gamenight.model.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    List<UserGame> findByUserIdOrderByGameTitleAsc(Long userId);
    List<UserGame> findByUserIdIn(List<Long> userIds);
    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    long deleteByUserIdAndGameId(Long userId, Long gameId);
}

