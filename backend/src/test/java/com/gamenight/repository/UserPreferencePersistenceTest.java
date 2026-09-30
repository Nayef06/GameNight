package com.gamenight.repository;

import com.gamenight.model.AppUser;
import com.gamenight.model.MultiplayerPreference;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserPreferencePersistenceTest {
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void preferencesPersistAndRemainScopedToTheirUser() {
        AppUser alex = new AppUser("alex", "hash");
        alex.updatePreferences(Set.of("Survival", "Shooter"),
                MultiplayerPreference.MULTIPLAYER, 4);
        Long alexId = userRepository.saveAndFlush(alex).getId();

        Long samId = userRepository.saveAndFlush(new AppUser("sam", "hash")).getId();
        entityManager.clear();

        AppUser savedAlex = userRepository.findById(alexId).orElseThrow();
        AppUser savedSam = userRepository.findById(samId).orElseThrow();

        assertThat(savedAlex.getPreferredGenres()).containsExactlyInAnyOrder("Survival", "Shooter");
        assertThat(savedAlex.getMultiplayerPreference()).isEqualTo(MultiplayerPreference.MULTIPLAYER);
        assertThat(savedAlex.getPreferredPlayerCount()).isEqualTo(4);
        assertThat(savedSam.getPreferredGenres()).isEmpty();
        assertThat(savedSam.getMultiplayerPreference()).isEqualTo(MultiplayerPreference.NO_PREFERENCE);
        assertThat(savedSam.getPreferredPlayerCount()).isNull();
    }
}
