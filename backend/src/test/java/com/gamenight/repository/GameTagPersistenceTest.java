package com.gamenight.repository;

import com.gamenight.model.Game;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameTagPersistenceTest {
    @Autowired private GameRepository gameRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void multipleTagsSurviveAReload() {
        Game game = gameRepository.saveAndFlush(new Game(
                "Terraria", "terraria", Set.of("Survival", "Sandbox", "Adventure"), true, 8));
        Long id = game.getId();
        entityManager.clear();

        assertThat(gameRepository.findById(id).orElseThrow().getTags())
                .isEqualTo(List.of("Adventure", "Sandbox", "Survival"));
    }
}
