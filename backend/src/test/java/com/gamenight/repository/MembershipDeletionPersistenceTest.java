package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GamingPlatform;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.UserGame;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MembershipDeletionPersistenceTest {
    @Autowired private UserRepository userRepository;
    @Autowired private GameRepository gameRepository;
    @Autowired private UserGameRepository userGameRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMembershipRepository membershipRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void removingAGameReturnsTheDeletedRowCount() {
        AppUser user = userRepository.save(new AppUser("alex", "hash"));
        Game game = gameRepository.save(new Game(
                "Terraria", "terraria", Set.of("Survival", "Sandbox"), true, 8));
        userGameRepository.saveAndFlush(new UserGame(user, game));

        assertThat(userGameRepository.deleteByUserIdAndGameIdAndPlatform(user.getId(), game.getId(), GamingPlatform.OTHER)).isEqualTo(1);
        entityManager.clear();
        assertThat(userGameRepository.existsByUserIdAndGameIdAndPlatform(user.getId(), game.getId(), GamingPlatform.OTHER)).isFalse();
        assertThat(userGameRepository.deleteByUserIdAndGameIdAndPlatform(user.getId(), game.getId(), GamingPlatform.OTHER)).isZero();
    }

    @Test
    void removingAGroupMemberReturnsTheDeletedRowCount() {
        AppUser creator = userRepository.save(new AppUser("creator", "hash"));
        AppUser member = userRepository.save(new AppUser("member", "hash"));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC123", creator));
        membershipRepository.saveAndFlush(new GroupMembership(member, group));

        assertThat(membershipRepository.deleteByUserIdAndGroupId(member.getId(), group.getId()))
                .isEqualTo(1);
        entityManager.clear();
        assertThat(membershipRepository.existsByUserIdAndGroupId(member.getId(), group.getId()))
                .isFalse();
    }
}
