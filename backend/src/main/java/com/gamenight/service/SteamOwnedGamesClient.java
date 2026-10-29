package com.gamenight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class SteamOwnedGamesClient {
    private static final String OWNED_GAMES_URL =
            "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/";

    public record OwnedGame(long appId, String name) {}

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SteamOwnedGamesClient(@Value("${steam.api.key:}") String apiKey, ObjectMapper objectMapper) {
        this(apiKey, objectMapper, createRestTemplate());
    }

    SteamOwnedGamesClient(String apiKey, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public List<OwnedGame> getOwnedGames(String steamId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Steam API configuration is missing.");
        }
        String inputJson = "{\"steamid\":\"" + steamId + "\",\"include_appinfo\":true}";
        URI uri = URI.create(OWNED_GAMES_URL
                + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)
                + "&input_json=" + URLEncoder.encode(inputJson, StandardCharsets.UTF_8));
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Unable to retrieve Steam library.");
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root == null ? null : root.path("response");
            if (data == null || !data.isObject() || !data.path("game_count").canConvertToInt()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "This Steam library may be private or unavailable.");
            }
            int count = data.path("game_count").asInt();
            JsonNode games = data.path("games");
            if (count < 0 || (count > 0 && !games.isArray())
                    || (games.isArray() && games.size() != count)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Unable to retrieve Steam library.");
            }
            if (count == 0) {
                return List.of();
            }
            List<OwnedGame> ownedGames = new ArrayList<>();
            for (JsonNode game : games) {
                JsonNode appId = game.path("appid");
                JsonNode name = game.path("name");
                if (!appId.canConvertToLong() || appId.asLong() <= 0
                        || !name.isTextual() || name.asText().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Unable to retrieve Steam library.");
                }
                ownedGames.add(new OwnedGame(appId.asLong(), name.asText().trim()));
            }
            return ownedGames;
        } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Steam API is temporarily unavailable.");
        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve Steam library.");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Steam API is temporarily unavailable.");
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to retrieve Steam library.");
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
