package com.gamenight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SteamOwnedGamesClientTest {
    private MockRestServiceServer server;
    private SteamOwnedGamesClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new SteamOwnedGamesClient("test-key", new ObjectMapper(), restTemplate);
    }

    @Test
    void readsAppIdsAndNamesFromOwnedGames() {
        server.expect(once(), requestTo(startsWith(
                        "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/")))
                .andExpect(request -> assertThat(request.getURI().getRawQuery())
                        .contains("key=test-key", "input_json="))
                .andRespond(withSuccess("""
                        {"response":{"game_count":2,"games":[
                          {"appid":105600,"name":"Terraria"},
                          {"appid":892970,"name":"Valheim"}
                        ]}}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        assertThat(client.getOwnedGames("76561198000000000")).containsExactly(
                new SteamOwnedGamesClient.OwnedGame(105600, "Terraria"),
                new SteamOwnedGamesClient.OwnedGame(892970, "Valheim"));
        server.verify();
    }

    @Test
    void distinguishesPrivateFromConfirmedEmptyLibrary() {
        server.expect(requestTo(startsWith("https://api.steampowered.com/")))
                .andRespond(withSuccess("{\"response\":{}}", org.springframework.http.MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getOwnedGames("76561198000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("private or unavailable");
        server.verify();

        server.reset();
        server.expect(requestTo(startsWith("https://api.steampowered.com/")))
                .andRespond(withSuccess("{\"response\":{\"game_count\":0}}",
                        org.springframework.http.MediaType.APPLICATION_JSON));
        assertThat(client.getOwnedGames("76561198000000000")).isEmpty();
        server.verify();
    }

    @Test
    void serverFailureHasSafeMessage() {
        server.expect(requestTo(startsWith("https://api.steampowered.com/")))
                .andRespond(withServerError());
        assertThatThrownBy(() -> client.getOwnedGames("76561198000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("temporarily unavailable")
                .hasMessageNotContaining("test-key");
        server.verify();
    }

    @Test
    void missingApiKeyFailsBeforeRequest() {
        SteamOwnedGamesClient unconfigured = new SteamOwnedGamesClient("", new ObjectMapper(),
                new RestTemplate());
        assertThatThrownBy(() -> unconfigured.getOwnedGames("76561198000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("configuration is missing");
    }
}
