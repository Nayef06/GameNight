package com.gamenight.service;

import com.gamenight.model.AppUser;
import com.gamenight.model.Game;
import com.gamenight.model.GameGroup;
import com.gamenight.model.GroupMembership;
import com.gamenight.model.UserGame;
import com.gamenight.repository.GameRepository;
import com.gamenight.repository.GroupMembershipRepository;
import com.gamenight.repository.GroupRepository;
import com.gamenight.repository.UserGameRepository;
import com.gamenight.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:steamtests;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "nayef")
class SteamLibraryIntegrationTest {
    private static final String STEAM_ID = "76561198000000000";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private GameRepository gameRepository;
    @Autowired private UserGameRepository userGameRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private GroupMembershipRepository membershipRepository;
    @MockitoBean private SteamOwnedGamesClient steamClient;

    private AppUser nayef;
    private AppUser alex;

    @BeforeEach
    void setUp() {
        nayef = userRepository.save(new AppUser("nayef", "unused"));
        alex = userRepository.save(new AppUser("alex", "unused"));
    }

    @Test
    void steamIdIsSavedOnlyOnAuthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/users/me/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamId\":\"" + STEAM_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId").value(STEAM_ID));
        mockMvc.perform(get("/api/users/me/steam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamId").value(STEAM_ID));

        assertThat(userRepository.findById(nayef.getId()).orElseThrow().getSteamId()).isEqualTo(STEAM_ID);
        assertThat(userRepository.findById(alex.getId()).orElseThrow().getSteamId()).isNull();
    }

    @Test
    void invalidSteamIdIsRejected() throws Exception {
        mockMvc.perform(put("/api/users/me/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"steamId\":\"not-an-id\"}"))
                .andExpect(status().isBadRequest());
        assertThat(nayef.getSteamId()).isNull();
    }

    @Test
    void importReusesManualGameAndRepeatedSyncIsAdditive() throws Exception {
        Game terraria = gameRepository.findByNormalizedTitle("terraria").orElseThrow();
        userGameRepository.save(new UserGame(nayef, terraria));
        saveSteamId();
        when(steamClient.getOwnedGames(STEAM_ID)).thenReturn(List.of(
                new SteamOwnedGamesClient.OwnedGame(105600, "Terraria"),
                new SteamOwnedGamesClient.OwnedGame(892970, "Valheim")));

        mockMvc.perform(post("/api/library/steam/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.alreadyInLibrary").value(1));
        entityManager.flush();
        entityManager.clear();
        assertThat(gameRepository.findBySteamAppId(105600L).orElseThrow().getId())
                .isEqualTo(terraria.getId());
        assertThat(gameRepository.findBySteamAppId(105600L).orElseThrow().getTags())
                .contains("Survival");
        assertThat(gameRepository.findBySteamAppId(892970L).orElseThrow().getTags())
                .containsExactly("Other");
        assertThat(userGameRepository.findByUserIdOrderByGameTitleAsc(nayef.getId())).hasSize(2);

        mockMvc.perform(post("/api/library/steam/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.alreadyInLibrary").value(2));
        assertThat(userGameRepository.findByUserIdOrderByGameTitleAsc(nayef.getId())).hasSize(2);

        Game minecraft = gameRepository.findByNormalizedTitle("minecraft").orElseThrow();
        mockMvc.perform(post("/api/library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gameId\":" + minecraft.getId() + "}"))
                .andExpect(status().isCreated());
        assertThat(userGameRepository.findByUserIdOrderByGameTitleAsc(nayef.getId())).hasSize(3);
    }

    @Test
    void importedGamesAppearInExistingSharedGameCalculation() throws Exception {
        Game terraria = gameRepository.findByNormalizedTitle("terraria").orElseThrow();
        Game lethal = gameRepository.findByNormalizedTitle("lethal company").orElseThrow();
        userGameRepository.save(new UserGame(alex, terraria));
        userGameRepository.save(new UserGame(alex, lethal));
        GameGroup group = groupRepository.save(new GameGroup("Friday", "ABC234", nayef));
        membershipRepository.save(new GroupMembership(nayef, group));
        membershipRepository.save(new GroupMembership(alex, group));
        saveSteamId();
        when(steamClient.getOwnedGames(STEAM_ID)).thenReturn(List.of(
                new SteamOwnedGamesClient.OwnedGame(105600, "Terraria"),
                new SteamOwnedGamesClient.OwnedGame(1966720, "Lethal Company"),
                new SteamOwnedGamesClient.OwnedGame(892970, "Valheim")));

        mockMvc.perform(post("/api/library/steam/sync")).andExpect(status().isOk());
        mockMvc.perform(get("/api/groups/{groupId}/shared-games", group.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Terraria", "Lethal Company")));
    }

    @Test
    void failedImportKeepsManualLibraryEntry() throws Exception {
        Game terraria = gameRepository.findByNormalizedTitle("terraria").orElseThrow();
        userGameRepository.save(new UserGame(nayef, terraria));
        saveSteamId();
        when(steamClient.getOwnedGames(STEAM_ID)).thenThrow(new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "This Steam library may be private or unavailable."));

        mockMvc.perform(post("/api/library/steam/sync"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("This Steam library may be private or unavailable."));
        assertThat(userGameRepository.findByUserIdOrderByGameTitleAsc(nayef.getId()))
                .extracting(userGame -> userGame.getGame().getId())
                .containsExactly(terraria.getId());
    }

    private void saveSteamId() {
        nayef.setSteamId(STEAM_ID);
        userRepository.save(nayef);
    }
}
