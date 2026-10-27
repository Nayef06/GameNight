package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GameNightSessionStatus;
import com.gamenight.model.SessionVote;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class SessionVotePersistenceTest {
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private GameNightSessionRepository sessionRepository;
    @Autowired private SessionVoteRepository voteRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void votePersistsAndChangingItKeepsOneRecord() {
        AppUser user = userRepository.save(new AppUser("voter", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", user));
        Game first = gameRepository.save(new Game("Terraria", "terraria", "Survival", true, 8));
        Game second = gameRepository.save(new Game("Minecraft", "minecraft", "Survival", true, 8));
        GameNightSession session = sessionRepository.save(new GameNightSession(group, user, Set.of(first, second)));
        Long voteId = voteRepository.saveAndFlush(new SessionVote(session, user, first)).getId();
        entityManager.clear();

        SessionVote saved = voteRepository.findBySessionIdAndUserId(session.getId(), user.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(voteId);
        assertThat(saved.getGame().getTitle()).isEqualTo("Terraria");
        assertThat(saved.getCreatedAt()).isNotNull();
        saved.changeGame(gameRepository.findById(second.getId()).orElseThrow());
        voteRepository.flush();
        entityManager.clear();

        assertThat(voteRepository.findBySessionId(session.getId())).hasSize(1)
                .first().extracting(vote -> vote.getGame().getTitle()).isEqualTo("Minecraft");
    }

    @Test
    void databaseRejectsSecondVoteFromSameUserInSession() {
        AppUser user = userRepository.save(new AppUser("voter", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", user));
        Game first = gameRepository.save(new Game("Terraria", "terraria", "Survival", true, 8));
        Game second = gameRepository.save(new Game("Minecraft", "minecraft", "Survival", true, 8));
        GameNightSession session = sessionRepository.save(new GameNightSession(group, user, Set.of(first, second)));
        voteRepository.saveAndFlush(new SessionVote(session, user, first));

        assertThatThrownBy(() -> voteRepository.saveAndFlush(new SessionVote(session, user, second)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void closedSessionPersistsAndAnotherCanBeOpened() {
        AppUser user = userRepository.save(new AppUser("voter", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", user));
        Game first = gameRepository.save(new Game("Terraria", "terraria", "Survival", true, 8));
        Game second = gameRepository.save(new Game("Minecraft", "minecraft", "Survival", true, 8));
        GameNightSession session = sessionRepository.save(new GameNightSession(group, user, Set.of(first, second)));
        session.close();
        sessionRepository.flush();
        entityManager.clear();

        assertThat(sessionRepository.findById(session.getId()).orElseThrow().getStatus())
                .isEqualTo(GameNightSessionStatus.CLOSED);
        GameNightSession next = sessionRepository.saveAndFlush(new GameNightSession(group, user, Set.of(first, second)));
        assertThat(sessionRepository.findByGroupIdAndStatus(group.getId(), GameNightSessionStatus.OPEN))
                .contains(next);
    }
}
