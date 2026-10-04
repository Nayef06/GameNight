package com.gamenight.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "group_game_selections")
public class GroupGameSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GameGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "selected_at", nullable = false, updatable = false)
    private Instant selectedAt;

    protected GroupGameSelection() {}

    public GroupGameSelection(GameGroup group, Game game) {
        this(group, game, Instant.now());
    }

    public GroupGameSelection(GameGroup group, Game game, Instant selectedAt) {
        this.group = group;
        this.game = game;
        this.selectedAt = selectedAt;
    }

    public Long getId() { return id; }
    public GameGroup getGroup() { return group; }
    public Game getGame() { return game; }
    public Instant getSelectedAt() { return selectedAt; }
}
