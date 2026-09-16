package com.gamenight.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected Game() {}

    public Game(String title, String normalizedTitle) {
        this.title = title;
        this.normalizedTitle = normalizedTitle;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getNormalizedTitle() { return normalizedTitle; }
}

