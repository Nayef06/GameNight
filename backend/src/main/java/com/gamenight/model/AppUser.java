package com.gamenight.model;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_preferred_genres", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "genre", nullable = false, length = 50)
    private Set<String> preferredTags = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "multiplayer_preference", nullable = false,
            columnDefinition = "varchar(30) default 'NO_PREFERENCE'")
    private MultiplayerPreference multiplayerPreference = MultiplayerPreference.NO_PREFERENCE;

    @Column(name = "preferred_player_count")
    private Integer preferredPlayerCount;

    @Column(name = "steam_id", length = 17)
    private String steamId;

    protected AppUser() {}

    public AppUser(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<String> getPreferredTags() { return Set.copyOf(preferredTags); }
    public MultiplayerPreference getMultiplayerPreference() { return multiplayerPreference; }
    public Integer getPreferredPlayerCount() { return preferredPlayerCount; }
    public String getSteamId() { return steamId; }

    public void setSteamId(String steamId) { this.steamId = steamId; }

    public void updatePreferences(Set<String> tags, MultiplayerPreference preference,
                                  Integer playerCount) {
        preferredTags.clear();
        preferredTags.addAll(tags);
        multiplayerPreference = preference;
        preferredPlayerCount = playerCount;
    }
}
