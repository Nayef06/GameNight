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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "session_votes", uniqueConstraints =
        @UniqueConstraint(columnNames = {"session_id", "user_id"}))
public class SessionVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameNightSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SessionVote() {}

    public SessionVote(GameNightSession session, AppUser user, Game game) {
        this.session = session;
        this.user = user;
        this.game = game;
    }

    public Long getId() { return id; }
    public GameNightSession getSession() { return session; }
    public AppUser getUser() { return user; }
    public Game getGame() { return game; }
    public Instant getCreatedAt() { return createdAt; }

    public void changeGame(Game game) { this.game = game; }
}
