package com.gamenight.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class DiscordWebhookClient {
    private static final Pattern WEBHOOK_URL = Pattern.compile(
            "https://(?:discord\\.com|discordapp\\.com)/api(?:/v10)?/webhooks/[0-9]{17,20}/[A-Za-z0-9_-]{20,200}");
    private final RestTemplate restTemplate;

    @Autowired
    public DiscordWebhookClient() {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restTemplate = new RestTemplate(factory);
    }

    DiscordWebhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static boolean isValidWebhookUrl(String url) {
        return url != null && url.length() <= 300 && WEBHOOK_URL.matcher(url).matches();
    }

    public void send(String webhookUrl, String message) {
        if (!isValidWebhookUrl(webhookUrl)) {
            throw new IllegalArgumentException("Invalid Discord webhook destination");
        }
        var response = restTemplate.postForEntity(URI.create(webhookUrl + "?wait=true"), Map.of(
                "content", message, "allowed_mentions", Map.of("parse", List.of())), Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Discord rejected delivery");
        }
    }
}
