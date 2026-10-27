package com.gamenight.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "gamenight_sessions")
public class GameNightSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GameGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameNightSessionStatus status = GameNightSessionStatus.OPEN;

    @ManyToMany
    @JoinTable(name = "gamenight_session_candidates",
            joinColumns = @JoinColumn(name = "session_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "game_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "game_id"}))
    private Set<Game> candidates = new LinkedHashSet<>();

    protected GameNightSession() {}

    public GameNightSession(GameGroup group, AppUser createdBy, Set<Game> candidates) {
        this.group = group;
        this.createdBy = createdBy;
        this.candidates.addAll(candidates);
    }

    public Long getId() { return id; }
    public GameGroup getGroup() { return group; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public GameNightSessionStatus getStatus() { return status; }
    public Set<Game> getCandidates() { return candidates; }

    public void close() { status = GameNightSessionStatus.CLOSED; }
}
