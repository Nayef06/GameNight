package com.gamenight.service;

import com.gamenight.dto.PreferenceDtos.PreferencesResponse;
import com.gamenight.dto.PreferenceDtos.UpdatePreferencesRequest;
import com.gamenight.model.AppUser;
import com.gamenight.model.MultiplayerPreference;
import com.gamenight.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PreferenceService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final GameCatalog gameCatalog;

    public PreferenceService(CurrentUserService currentUserService, UserRepository userRepository,
                             GameCatalog gameCatalog) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.gameCatalog = gameCatalog;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences() {
        return toResponse(currentUserService.get());
    }

    @Transactional
    public PreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
        if (request.preferredPlayerCount() != null && request.preferredPlayerCount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Preferred player count must be positive.");
        }

        Set<String> tags = new LinkedHashSet<>(request.preferredTags());
        if (tags.contains(null) || tags.stream().anyMatch(tag -> !gameCatalog.getTags().contains(tag))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Preferred tags must use supported tag values.");
        }

        MultiplayerPreference multiplayerPreference;
        try {
            multiplayerPreference = MultiplayerPreference.valueOf(request.multiplayerPreference());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Multiplayer preference must be NO_PREFERENCE, MULTIPLAYER, or SINGLE_PLAYER.");
        }

        AppUser user = currentUserService.get();
        user.updatePreferences(tags, multiplayerPreference, request.preferredPlayerCount());
        userRepository.save(user);
        return toResponse(user);
    }

    private PreferencesResponse toResponse(AppUser user) {
        List<String> tags = user.getPreferredTags().stream()
                .sorted()
                .toList();
        return new PreferencesResponse(tags, user.getMultiplayerPreference().name(),
                user.getPreferredPlayerCount());
    }
}
