package com.gamenight.repository;

import com.gamenight.model.Game;
import com.gamenight.model.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    List<UserGame> findByUserIdOrderByGameTitleAsc(Long userId);
    List<UserGame> findByUserIdIn(List<Long> userIds);
    boolean existsByUserIdAndGameId(Long userId, Long gameId);
    long countByGameIdAndUserIdIn(Long gameId, List<Long> userIds);
    @Modifying
    @Query("delete from UserGame ownership where ownership.user.id = :userId and ownership.game.id = :gameId")
    int deleteByUserIdAndGameId(@Param("userId") Long userId, @Param("gameId") Long gameId);
}
