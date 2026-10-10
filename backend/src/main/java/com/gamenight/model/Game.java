package com.gamenight.model;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "normalized_title", nullable = false, unique = true, length = 120)
    private String normalizedTitle;

    @Column(nullable = false, length = 50, columnDefinition = "varchar(50) default 'Other'")
    private String genre = "Other";

    @ElementCollection
    @CollectionTable(name = "game_tags", joinColumns = @JoinColumn(name = "game_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "multiplayer_support", nullable = false, columnDefinition = "boolean default false")
    private boolean multiplayerSupport;

    @Column(name = "max_players", nullable = false, columnDefinition = "integer default 1")
    private int maxPlayers = 1;

    protected Game() {}

    public Game(String title, String normalizedTitle, String genre, boolean multiplayerSupport, int maxPlayers) {
        this.title = title;
        this.normalizedTitle = normalizedTitle;
        this.genre = genre;
        this.multiplayerSupport = multiplayerSupport;
        this.maxPlayers = maxPlayers;
    }

    public Game(String title, String normalizedTitle, Set<String> tags,
                boolean multiplayerSupport, int maxPlayers) {
        this(title, normalizedTitle, tags.iterator().next(), multiplayerSupport, maxPlayers);
        this.tags.addAll(tags);
    }

    public void updateCatalogMetadata(String title, Set<String> tags,
                                      boolean multiplayerSupport, int maxPlayers) {
        this.title = title;
        this.genre = tags.iterator().next(); // Keep the legacy non-null column populated.
        this.tags.clear();
        this.tags.addAll(tags);
        this.multiplayerSupport = multiplayerSupport;
        this.maxPlayers = maxPlayers;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public List<String> getTags() {
        return tags.isEmpty() ? List.of(genre) : tags.stream().sorted().toList();
    }
    public boolean isMultiplayerSupport() { return multiplayerSupport; }
    public int getMaxPlayers() { return maxPlayers; }
}
