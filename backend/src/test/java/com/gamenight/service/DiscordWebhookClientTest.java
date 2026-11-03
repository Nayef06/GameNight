package com.gamenight.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DiscordWebhookClientTest {
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final DiscordWebhookClient client = new DiscordWebhookClient(http);
    private final String webhook = PlatformAndDiscordIntegrationTest.WEBHOOK;

    @Test
    void sendsPlainJsonMessageWithMentionsDisabledAndChecksDelivery() {
        server.expect(requestTo(webhook + "?wait=true")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"content":"GameNight: Voting has ended. Most voted game: Terraria.",
                         "allowed_mentions":{"parse":[]}}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        client.send(webhook, "GameNight: Voting has ended. Most voted game: Terraria.");
        server.verify();
    }

    @Test
    void rejectsRedirectsWithoutTreatingThemAsSuccessfulDelivery() {
        server.expect(requestTo(webhook + "?wait=true")).andRespond(withStatus(HttpStatus.FOUND)
                .header("Location", "https://example.com/"));
        assertThatThrownBy(() -> client.send(webhook, "GameNight"))
                .isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test
    void acceptsOnlyDiscordWebhookEndpointsEvenAtSendTime() {
        assertThat(DiscordWebhookClient.isValidWebhookUrl(webhook)).isTrue();
        assertThat(DiscordWebhookClient.isValidWebhookUrl(webhook.replace("discord.com/api/", "discord.com/api/v10/"))).isTrue();
        assertThat(DiscordWebhookClient.isValidWebhookUrl(webhook.replace("discord.com", "discordapp.com"))).isTrue();
        assertThatThrownBy(() -> client.send("https://example.com/", "GameNight"))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }
}
