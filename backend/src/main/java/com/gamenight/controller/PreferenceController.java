package com.gamenight.controller;

import com.gamenight.dto.PreferenceDtos.PreferencesResponse;
import com.gamenight.dto.PreferenceDtos.UpdatePreferencesRequest;
import com.gamenight.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
public class PreferenceController {
    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PreferencesResponse getPreferences() {
        return preferenceService.getPreferences();
    }

    @PutMapping
    public PreferencesResponse updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return preferenceService.updatePreferences(request);
    }
}
