package com.gamenight.repository;

import com.gamenight.model.GroupGameSelection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupGameSelectionRepository extends JpaRepository<GroupGameSelection, Long> {
    List<GroupGameSelection> findTop3ByGroupIdOrderBySelectedAtDescIdDesc(Long groupId);
    List<GroupGameSelection> findTop5ByGroupIdOrderBySelectedAtDescIdDesc(Long groupId);
    long countByGroupId(Long groupId);

    interface GameSelectionCount {
        String getTitle();
        long getSelectionCount();
    }

    @Query("""
            select s.game.title as title, count(s) as selectionCount
            from GroupGameSelection s
            where s.group.id = :groupId
            group by s.game.id, s.game.title
            order by count(s) desc, lower(s.game.title) asc, s.game.title asc
            """)
    List<GameSelectionCount> findMostSelectedGames(@Param("groupId") Long groupId, Pageable pageable);
}
