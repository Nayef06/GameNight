package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GameNightSession;
import com.gamenight.model.GroupGameSelection;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.SessionVote;
import com.gamenight.repository.GameNightSessionRepository;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.GroupGameSelectionRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import com.gamenight.repository.SessionVoteRepository;
import com.gamenight.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:groupstatistics;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class GroupStatisticsIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private GroupRepository groups;
    @Autowired private GroupMembershipRepository memberships;
    @Autowired private GroupGameSelectionRepository selections;
    @Autowired private GameNightSessionRepository sessions;
    @Autowired private SessionVoteRepository votes;
    @Autowired private GameRepository games;
    @Autowired private EntityManager entityManager;

    private AppUser creator;
    private AppUser member;
    private GameGroup group;
    private GameGroup otherGroup;
    private Game terraria;
    private Game minecraft;

    @BeforeEach
    void setUp() {
        creator = users.save(new AppUser("statisticscreator", "hash"));
        member = users.save(new AppUser("statisticsmember", "hash"));
        group = groups.save(new GameGroup("Friday", "STATS1", creator));
        otherGroup = groups.save(new GameGroup("Saturday", "STATS2", creator));
        memberships.save(new GroupMembership(creator, group));
        memberships.save(new GroupMembership(member, group));
        memberships.save(new GroupMembership(creator, otherGroup));
        terraria = games.findByNormalizedTitle("terraria").orElseThrow();
        minecraft = games.findByNormalizedTitle("minecraft").orElseThrow();
    }

    @Test
    void countsOnlyClosedSessionsInTheRequestedGroup() throws Exception {
        session(group, true);
        session(group, true);
        session(group, false);
        session(otherGroup, true);

        statistics().andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessions").value(2))
                .andExpect(jsonPath("$.gamesSelected").value(0))
                .andExpect(jsonPath("$.mostSelectedGame").value(nullValue()));
    }

    @Test
    void countsTheEntireSelectionHistoryAndFindsTheMostSelectedGame() throws Exception {
        select(group, terraria, 4);
        select(group, minecraft, 3);
        select(otherGroup, minecraft, 6);

        statistics().andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessions").value(0))
                .andExpect(jsonPath("$.gamesSelected").value(7))
                .andExpect(jsonPath("$.mostSelectedGame.title").value("Terraria"))
                .andExpect(jsonPath("$.mostSelectedGame.count").value(4));
    }

    @Test
    void tiedSelectionCountsUseAlphabeticalOrderRegardlessOfInsertionOrderOrCase() throws Exception {
        Game zulu = games.save(new Game("Zulu Stats", "zulu stats", "Other", true, 4));
        Game alpha = games.save(new Game("alpha stats", "alpha stats", "Other", true, 4));
        select(group, zulu, 2);
        select(group, alpha, 2);

        statistics().andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesSelected").value(4))
                .andExpect(jsonPath("$.mostSelectedGame.title").value("alpha stats"))
                .andExpect(jsonPath("$.mostSelectedGame.count").value(2));
    }

    @Test
    void votesAndVotingWinnersDoNotCountAsSelections() throws Exception {
        GameNightSession session = session(group, true);
        votes.save(new SessionVote(session, creator, terraria));
        votes.save(new SessionVote(session, member, terraria));
        select(group, minecraft, 1);

        statistics().andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessions").value(1))
                .andExpect(jsonPath("$.gamesSelected").value(1))
                .andExpect(jsonPath("$.mostSelectedGame.title").value("Minecraft"))
                .andExpect(jsonPath("$.mostSelectedGame.count").value(1));
    }

    @Test
    void emptyGroupReturnsZeroCountsAndNoMostSelectedGame() throws Exception {
        statistics().andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessions").value(0))
                .andExpect(jsonPath("$.gamesSelected").value(0))
                .andExpect(jsonPath("$.mostSelectedGame").value(nullValue()));
    }

    @Test
    void nonMembersCannotRetrieveStatistics() throws Exception {
        AppUser outsider = users.save(new AppUser("statisticsoutsider", "hash"));
        mockMvc.perform(get("/api/groups/{id}/statistics", group.getId())
                        .with(user(outsider.getUsername())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/groups/{id}/statistics", otherGroup.getId())
                        .with(user(member.getUsername())))
                .andExpect(status().isForbidden());
        memberships.deleteByUserIdAndGroupId(member.getId(), group.getId());
        mockMvc.perform(get("/api/groups/{id}/statistics", group.getId())
                        .with(user(member.getUsername())))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingGroupReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/groups/{id}/statistics", Long.MAX_VALUE)
                        .with(user(member.getUsername())))
                .andExpect(status().isNotFound());
    }

    private ResultActions statistics() throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mockMvc.perform(get("/api/groups/{id}/statistics", group.getId())
                .with(user(member.getUsername())));
    }

    private GameNightSession session(GameGroup targetGroup, boolean closed) {
        GameNightSession session = new GameNightSession(targetGroup, creator, Set.of(terraria, minecraft));
        if (closed) session.close();
        return sessions.save(session);
    }

    private void select(GameGroup targetGroup, Game game, int count) {
        for (int index = 0; index < count; index++) {
            selections.save(new GroupGameSelection(targetGroup, game));
        }
    }
}
