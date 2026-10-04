package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupGameSelection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GroupGameSelectionRepositoryTest {
    @Autowired private UserRepository userRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupGameSelectionRepository selectionRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void recentSelectionsReturnOnlyFiveNewestInNewestFirstOrder() {
        AppUser creator = userRepository.save(new AppUser("creator", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", creator));

        for (int index = 1; index <= 6; index++) {
            Game game = gameRepository.save(new Game("Game " + index, "game " + index,
                    "Other", true, 4));
            selectionRepository.save(new GroupGameSelection(group, game,
                    Instant.parse("2026-09-0" + index + "T20:00:00Z")));
        }
        selectionRepository.flush();
        entityManager.clear();

        var recent = selectionRepository.findTop5ByGroupIdOrderBySelectedAtDescIdDesc(group.getId());

        assertThat(recent).hasSize(5);
        assertThat(recent).extracting(selection -> selection.getGame().getTitle())
                .containsExactly("Game 6", "Game 5", "Game 4", "Game 3", "Game 2");
    }
}
