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
    public static final List<String> SUPPORTED_GENRES = List.of(
            "Action", "Adventure", "RPG", "Strategy", "Survival",
            "Shooter", "Sports", "Simulation", "Puzzle", "Other"
    );

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public PreferenceService(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
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

        Set<String> genres = new LinkedHashSet<>(request.preferredGenres());
        if (genres.stream().anyMatch(genre -> !SUPPORTED_GENRES.contains(genre))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Preferred genres must use supported genre values.");
        }

        MultiplayerPreference multiplayerPreference;
        try {
            multiplayerPreference = MultiplayerPreference.valueOf(request.multiplayerPreference());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Multiplayer preference must be NO_PREFERENCE, MULTIPLAYER, or SINGLE_PLAYER.");
        }

        AppUser user = currentUserService.get();
        user.updatePreferences(genres, multiplayerPreference, request.preferredPlayerCount());
        userRepository.save(user);
        return toResponse(user);
    }

    private PreferencesResponse toResponse(AppUser user) {
        List<String> genres = SUPPORTED_GENRES.stream()
                .filter(user.getPreferredGenres()::contains)
                .toList();
        return new PreferencesResponse(genres, user.getMultiplayerPreference().name(),
                user.getPreferredPlayerCount());
    }
}
