package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GameNightSessionPersistenceTest {
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GameNightSessionRepository sessionRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void sessionAndCandidatesRemainAfterReload() {
        AppUser creator = userRepository.save(new AppUser("creator", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", creator));
        Game terraria = gameRepository.save(new Game("Terraria", "terraria", "Survival", true, 8));
        Game minecraft = gameRepository.save(new Game("Minecraft", "minecraft", "Survival", true, 8));
        Long sessionId = sessionRepository.saveAndFlush(
                new GameNightSession(group, creator, Set.of(terraria, minecraft))).getId();
        entityManager.clear();

        GameNightSession saved = sessionRepository.findByIdAndGroupId(sessionId, group.getId()).orElseThrow();
        assertThat(saved.getGroup().getId()).isEqualTo(group.getId());
        assertThat(saved.getCreatedBy().getUsername()).isEqualTo("creator");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(GameNightSessionStatus.OPEN);
        assertThat(saved.getCandidates()).extracting(Game::getTitle)
                .containsExactlyInAnyOrder("Terraria", "Minecraft");
        assertThat(sessionRepository.findByGroupIdAndStatus(group.getId(), GameNightSessionStatus.OPEN))
                .isPresent();
    }
}
