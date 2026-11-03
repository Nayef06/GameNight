package com.gamenight.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_games", uniqueConstraints = @UniqueConstraint(
        name = "uk_user_game_platform", columnNames = {"user_id", "game_id", "platform"}))
public class UserGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'OTHER'")
    private GamingPlatform platform = GamingPlatform.OTHER;

    // Only pre-platform records need confirmation on their next Steam sync.
    @Column(name = "platform_unconfirmed", nullable = false, columnDefinition = "boolean default false")
    private boolean platformUnconfirmed;

    protected UserGame() {}

    public UserGame(AppUser user, Game game) {
        this(user, game, GamingPlatform.OTHER);
    }

    public UserGame(AppUser user, Game game, GamingPlatform platform) {
        this.user = user;
        this.game = game;
        this.platform = java.util.Objects.requireNonNull(platform);
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Game getGame() { return game; }
    public GamingPlatform getPlatform() { return platform; }
    public void confirmSteamPlatform() {
        platform = GamingPlatform.STEAM;
        platformUnconfirmed = false;
    }
}

