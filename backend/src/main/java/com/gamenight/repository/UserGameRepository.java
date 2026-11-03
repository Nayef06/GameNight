package com.gamenight.repository;

import com.gamenight.model.GamingPlatform;
import com.gamenight.model.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {
    List<UserGame> findByUserIdOrderByGameTitleAsc(Long userId);
    List<UserGame> findByUserIdIn(List<Long> userIds);
    boolean existsByUserIdAndGameIdAndPlatform(Long userId, Long gameId, GamingPlatform platform);
    Optional<UserGame> findByUserIdAndGameIdAndPlatformUnconfirmedTrue(Long userId, Long gameId);
    @Query("select count(distinct ownership.user.id) from UserGame ownership "
            + "where ownership.game.id = :gameId and ownership.user.id in :userIds")
    long countByGameIdAndUserIdIn(Long gameId, List<Long> userIds);
    @Modifying
    @Query("delete from UserGame ownership where ownership.user.id = :userId "
            + "and ownership.game.id = :gameId and ownership.platform = :platform")
    int deleteByUserIdAndGameIdAndPlatform(@Param("userId") Long userId, @Param("gameId") Long gameId,
                                         @Param("platform") GamingPlatform platform);
}
