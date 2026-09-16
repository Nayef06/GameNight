package com.gamenight.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "group_id"}))
public class GroupMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GameGroup group;

    protected GroupMembership() {}

    public GroupMembership(AppUser user, GameGroup group) {
        this.user = user;
        this.group = group;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public GameGroup getGroup() { return group; }
}
