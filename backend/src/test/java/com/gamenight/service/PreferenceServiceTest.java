package com.gamenight.service;

import com.gamenight.dto.PreferenceDtos.PreferencesResponse;
import com.gamenight.dto.PreferenceDtos.UpdatePreferencesRequest;
import com.gamenight.model.AppUser;
import com.gamenight.model.MultiplayerPreference;
import com.gamenight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {
    @Mock private CurrentUserService currentUserService;
    @Mock private UserRepository userRepository;
    @Mock private GameCatalog gameCatalog;

    private PreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new PreferenceService(currentUserService, userRepository, gameCatalog);
        org.mockito.Mockito.lenient().when(gameCatalog.getTags())
                .thenReturn(List.of("Survival", "Shooter", "Strategy", "Co-op"));
    }

    @Test
    void newUserHasNeutralDefaults() {
        AppUser user = new AppUser("alex", "hash");
        when(currentUserService.get()).thenReturn(user);

        PreferencesResponse response = preferenceService.getPreferences();

        assertThat(response).isEqualTo(
                new PreferencesResponse(List.of(), "NO_PREFERENCE", null));
    }

    @Test
    void userCanSaveAndRetrievePreferredTags() {
        AppUser user = new AppUser("alex", "hash");
        when(currentUserService.get()).thenReturn(user);

        preferenceService.updatePreferences(request(
                List.of("Survival", "Shooter"), "NO_PREFERENCE", null));
        PreferencesResponse response = preferenceService.getPreferences();

        assertThat(response.preferredTags()).containsExactly("Shooter", "Survival");
        verify(userRepository).save(user);
    }

    @Test
    void userCanSaveMultiplayerPreference() {
        AppUser user = new AppUser("alex", "hash");
        when(currentUserService.get()).thenReturn(user);

        PreferencesResponse response = preferenceService.updatePreferences(request(
                List.of(), "MULTIPLAYER", null));

        assertThat(response.multiplayerPreference()).isEqualTo("MULTIPLAYER");
        assertThat(user.getMultiplayerPreference()).isEqualTo(MultiplayerPreference.MULTIPLAYER);
    }

    @Test
    void userCanSavePreferredPlayerCount() {
        AppUser user = new AppUser("alex", "hash");
        when(currentUserService.get()).thenReturn(user);

        PreferencesResponse response = preferenceService.updatePreferences(request(
                List.of(), "NO_PREFERENCE", 4));

        assertThat(response.preferredPlayerCount()).isEqualTo(4);
        assertThat(user.getPreferredPlayerCount()).isEqualTo(4);
    }

    @Test
    void oneUsersPreferencesDoNotAffectAnotherUser() {
        AppUser alex = new AppUser("alex", "hash");
        AppUser sam = new AppUser("sam", "hash");
        when(currentUserService.get()).thenReturn(alex, sam);

        preferenceService.updatePreferences(request(
                List.of("Strategy"), "SINGLE_PLAYER", 1));
        PreferencesResponse samPreferences = preferenceService.getPreferences();

        assertThat(samPreferences).isEqualTo(
                new PreferencesResponse(List.of(), "NO_PREFERENCE", null));
    }

    @Test
    void invalidPlayerCountIsRejected() {
        assertThatThrownBy(() -> preferenceService.updatePreferences(request(
                List.of(), "NO_PREFERENCE", 0)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Preferred player count must be positive.");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unsupportedTagAndMultiplayerPreferenceAreRejected() {
        assertThatThrownBy(() -> preferenceService.updatePreferences(request(
                List.of("Unknown"), "NO_PREFERENCE", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("supported tag values");
        assertThatThrownBy(() -> preferenceService.updatePreferences(request(
                List.of(), "CO_OP_ONLY", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("NO_PREFERENCE, MULTIPLAYER, or SINGLE_PLAYER");
    }

    private UpdatePreferencesRequest request(List<String> genres, String multiplayer, Integer count) {
        return new UpdatePreferencesRequest(genres, multiplayer, count);
    }
}
