package com.gamenight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamenight.model.*;
import com.gamenight.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Deliberately no test transaction: webhook listeners must run after the real API transaction commits.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:platformdiscord;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PlatformAndDiscordIntegrationTest {
    static final String WEBHOOK = "https://discord.com/api/webhooks/123456789012345678/secret_test_token_abcdefghijklmnopqrstuvwxyz";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired GroupRepository groups;
    @Autowired GroupMembershipRepository memberships;
    @Autowired UserGameRepository ownerships;
    @Autowired GameRepository games;
    @Autowired GameNightSessionRepository sessions;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SteamOwnedGamesClient steam;
    @MockitoBean DiscordWebhookClient discord;

    AppUser creator;
    AppUser member;
    GameGroup group;
    Game terraria;
    Game minecraft;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        creator = users.save(new AppUser("creator" + suffix, "hash"));
        member = users.save(new AppUser("member" + suffix, "hash"));
        group = groups.save(new GameGroup("Friday Night Group", suffix.substring(0, 6), creator));
        memberships.save(new GroupMembership(creator, group));
        memberships.save(new GroupMembership(member, group));
        terraria = games.findByNormalizedTitle("terraria").orElseThrow();
        minecraft = games.findByNormalizedTitle("minecraft").orElseThrow();
        for (AppUser owner : List.of(creator, member)) {
            ownerships.save(new UserGame(owner, terraria, GamingPlatform.STEAM));
            ownerships.save(new UserGame(owner, minecraft, GamingPlatform.STEAM));
        }
    }

    @Test
    void creatorCanSaveAndRemoveSecretButMembersAndOutsidersCannot() throws Exception {
        saveWebhook();
        assertThat(groups.findById(group.getId()).orElseThrow().getDiscordWebhookUrl()).isEqualTo(WEBHOOK);
        for (AppUser viewer : List.of(creator, member)) {
            mvc.perform(get("/api/groups/{id}", group.getId()).with(user(viewer.getUsername())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.discordWebhookConfigured").value(true))
                    .andExpect(jsonPath("$.discordWebhookUrl").doesNotExist())
                    .andExpect(content().string(not(containsString("secret_test_token"))));
            mvc.perform(get("/api/groups").with(user(viewer.getUsername())))
                    .andExpect(content().string(not(containsString("secret_test_token"))));
        }
        AppUser outsider = users.save(new AppUser("outsider" + creator.getId(), "hash"));
        for (String username : List.of(member.getUsername(), outsider.getUsername())) {
            mvc.perform(put(webhookPath()).with(user(username)).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("webhookUrl", WEBHOOK))))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(webhookPath()).with(user(username))).andExpect(status().isForbidden());
        }
        mvc.perform(delete(webhookPath()).with(user(creator.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discordWebhookConfigured").value(false));
        assertThat(groups.findById(group.getId()).orElseThrow().getDiscordWebhookUrl()).isNull();
        createSession();
        verifyNoInteractions(discord);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://discord.com/api/webhooks/123456789012345678/token",
            "https://example.com/api/webhooks/123456789012345678/token",
            "https://discord.com.evil.test/api/webhooks/123456789012345678/token",
            "https://discord.com@127.0.0.1/api/webhooks/123456789012345678/token",
            "https://discord.com:8443/api/webhooks/123456789012345678/token",
            "https://discord.com/api/webhooks/123456789012345678/token?redirect=evil",
            "https://discord.com/api/webhooks/123456789012345678/../../other",
            "https://discord.com/api/webhooks/123456789012345678/token#fragment"
    })
    void rejectsArbitraryOrMalformedWebhookDestinations(String url) throws Exception {
        mvc.perform(put(webhookPath()).with(user(creator.getUsername())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("webhookUrl", url))))
                .andExpect(status().isBadRequest());
        assertThat(groups.findById(group.getId()).orElseThrow().getDiscordWebhookUrl()).isNull();
    }

    @Test
    void announcesCreationAndWinnerButNotIndividualVotes() throws Exception {
        saveWebhook();
        long sessionId = createSession();
        verify(discord).send(WEBHOOK, "GameNight: A new voting session has started in Friday Night Group.");
        vote(sessionId, creator, terraria);
        vote(sessionId, member, terraria);
        verifyNoMoreInteractions(discord);
        close(sessionId);
        verify(discord).send(WEBHOOK, "GameNight: Voting has ended. Most voted game: Terraria.");
        mvc.perform(post(sessionPath(sessionId) + "/close").with(user(creator.getUsername())))
                .andExpect(status().isConflict());
        verifyNoMoreInteractions(discord);
    }

    @Test
    void announcesTiedGames() throws Exception {
        saveWebhook();
        long sessionId = createSession();
        vote(sessionId, creator, terraria);
        vote(sessionId, member, minecraft);
        close(sessionId);
        verify(discord).send(WEBHOOK, "GameNight: Voting has ended. Tie between: Minecraft, Terraria.");
    }

    @Test
    void announcesNoVotes() throws Exception {
        saveWebhook();
        long sessionId = createSession();
        close(sessionId);
        verify(discord).send(WEBHOOK, "GameNight: Voting has ended. No votes were submitted.");
    }

    @Test
    void discordFailureDoesNotUndoSessionOrVotesAndLogsWithoutSecrets(CapturedOutput output) throws Exception {
        saveWebhook();
        doThrow(new IllegalStateException("Connection failed " + WEBHOOK)).when(discord).send(anyString(), anyString());
        long sessionId = createSession();
        assertThat(sessions.findById(sessionId).orElseThrow().getStatus()).isEqualTo(GameNightSessionStatus.OPEN);
        vote(sessionId, member, terraria);
        close(sessionId);
        assertThat(sessions.findById(sessionId).orElseThrow().getStatus()).isEqualTo(GameNightSessionStatus.CLOSED);
        mvc.perform(get(sessionPath(sessionId) + "/results").with(user(member.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.results[0].votes").value(1));
        assertThat(output).contains("Discord delivery failed for group " + group.getId())
                .doesNotContain("secret_test_token");
        assertThat(ownerships.findByUserIdOrderByGameTitleAsc(creator.getId())).hasSize(2);
    }

    @Test
    void allPlatformsPersistAndRemovingOneDoesNotRemoveOthers() throws Exception {
        long gameId = games.findByNormalizedTitle("portal 2").orElseThrow().getId();
        for (GamingPlatform platform : GamingPlatform.values()) {
            mvc.perform(post("/api/library").with(user(creator.getUsername())).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("gameId", gameId, "platform", platform))))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.platform").value(platform.name()));
        }
        mvc.perform(get("/api/library").with(user(creator.getUsername())))
                .andExpect(jsonPath("$[?(@.id == " + gameId + ")].platform", containsInAnyOrder(
                        "STEAM", "EPIC_GAMES", "XBOX", "PLAYSTATION", "NINTENDO_SWITCH", "OTHER")));
        assertThat(ownerships.findByUserIdOrderByGameTitleAsc(creator.getId()).stream()
                .filter(ownership -> ownership.getGame().getId().equals(gameId)).map(UserGame::getPlatform))
                .containsExactlyInAnyOrder(GamingPlatform.values());
        mvc.perform(post("/api/library").with(user(creator.getUsername())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("gameId", gameId, "platform", "XBOX"))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/library/" + gameId).param("platform", "XBOX").with(user(creator.getUsername())))
                .andExpect(status().isNoContent());
        assertThat(ownerships.findByUserIdOrderByGameTitleAsc(creator.getId())).hasSize(7);
        mvc.perform(post("/api/library").with(user(creator.getUsername())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("gameId", gameId, "platform", "INVALID"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sharedGamesCountDistinctMembersAcrossPlatformsAndStillSupportSelectionAndVoting() throws Exception {
        jdbc.update("update user_games set platform = 'XBOX' where user_id = ? and game_id = ?", member.getId(), terraria.getId());
        ownerships.save(new UserGame(creator, terraria, GamingPlatform.PLAYSTATION));
        // Two copies owned by just one member must not masquerade as shared ownership.
        Game portal = games.findByNormalizedTitle("portal 2").orElseThrow();
        ownerships.save(new UserGame(creator, portal, GamingPlatform.STEAM));
        ownerships.save(new UserGame(creator, portal, GamingPlatform.XBOX));
        mvc.perform(get("/api/groups/{id}/shared-games", group.getId()).with(user(member.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == 'Terraria')].platforms[0]").value("STEAM"))
                .andExpect(jsonPath("$[?(@.title == 'Terraria')].platforms[1]").value("XBOX"))
                .andExpect(jsonPath("$[?(@.title == 'Terraria')].platforms[2]").value("PLAYSTATION"));
        mvc.perform(get("/api/groups/{id}/shared-games", group.getId()).param("tags", "Survival")
                        .param("minPlayers", "4").param("multiplayerSupport", "true").with(user(member.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].title", hasItem("Terraria")));
        mvc.perform(post("/api/groups/{id}/selections", group.getId()).with(user(member.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gameId\":" + terraria.getId() + "}"))
                .andExpect(status().isCreated());
        long sessionId = createSession();
        vote(sessionId, member, terraria);
        close(sessionId);
        verifyNoInteractions(discord);
    }

    @Test
    void syncPreservesOtherPlatformsAndConfirmsOnlyLegacyUnknownEntries() throws Exception {
        creator.setSteamId("76561198000000000");
        users.save(creator);
        jdbc.update("update user_games set platform = 'OTHER', platform_unconfirmed = true where user_id = ? and game_id = ?",
                creator.getId(), terraria.getId());
        jdbc.update("update user_games set platform = 'XBOX' where user_id = ? and game_id = ?", creator.getId(), minecraft.getId());
        ownerships.save(new UserGame(creator, terraria, GamingPlatform.XBOX));
        ownerships.save(new UserGame(creator, minecraft, GamingPlatform.OTHER));
        when(steam.getOwnedGames(creator.getSteamId())).thenReturn(List.of(
                new SteamOwnedGamesClient.OwnedGame(105600L, "Terraria"),
                new SteamOwnedGamesClient.OwnedGame(1L, "Minecraft")));
        mvc.perform(post("/api/library/steam/sync").with(user(creator.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.alreadyInLibrary").value(1));
        mvc.perform(post("/api/library/steam/sync").with(user(creator.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.alreadyInLibrary").value(2));
        assertThat(ownerships.findByUserIdOrderByGameTitleAsc(creator.getId())).hasSize(5)
                .extracting(UserGame::getPlatform).containsExactlyInAnyOrder(
                        GamingPlatform.STEAM, GamingPlatform.XBOX, GamingPlatform.STEAM, GamingPlatform.XBOX,
                        GamingPlatform.OTHER);
    }

    private String webhookPath() { return "/api/groups/" + group.getId() + "/discord-webhook"; }
    private String sessionPath(long sessionId) { return "/api/groups/" + group.getId() + "/sessions/" + sessionId; }

    private void saveWebhook() throws Exception {
        mvc.perform(put(webhookPath()).with(user(creator.getUsername())).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("webhookUrl", WEBHOOK))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discordWebhookConfigured").value(true))
                .andExpect(content().string(not(containsString("secret_test_token"))));
    }

    private long createSession() throws Exception {
        String response = mvc.perform(post("/api/groups/{id}/sessions", group.getId()).with(user(creator.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("gameIds", List.of(terraria.getId(), minecraft.getId())))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("id").asLong();
    }

    private void vote(long sessionId, AppUser voter, Game game) throws Exception {
        mvc.perform(post(sessionPath(sessionId) + "/vote").with(user(voter.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"gameId\":" + game.getId() + "}"))
                .andExpect(status().isNoContent());
    }

    private void close(long sessionId) throws Exception {
        mvc.perform(post(sessionPath(sessionId) + "/close").with(user(creator.getUsername())))
                .andExpect(status().isNoContent());
    }
}
