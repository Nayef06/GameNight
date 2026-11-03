package com.gamenight.service;

import com.gamenight.dto.SessionDtos.VoteCount;
import com.gamenight.model.GameGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Service
public class DiscordNotificationService {
    private static final Logger log = LoggerFactory.getLogger(DiscordNotificationService.class);
    private final ApplicationEventPublisher events;
    private final DiscordWebhookClient client;

    public DiscordNotificationService(ApplicationEventPublisher events, DiscordWebhookClient client) {
        this.events = events;
        this.client = client;
    }

    public void sessionCreated(GameGroup group) {
        queue(group, "GameNight: A new voting session has started in " + group.getName() + ".");
    }

    public void sessionClosed(GameGroup group, List<VoteCount> results) {
        long mostVotes = results.stream().mapToLong(VoteCount::votes).max().orElse(0);
        String outcome;
        if (mostVotes == 0) {
            outcome = "No votes were submitted.";
        } else {
            List<String> winners = results.stream().filter(result -> result.votes() == mostVotes)
                    .map(VoteCount::title).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            outcome = (winners.size() == 1 ? "Most voted game: " : "Tie between: ")
                    + String.join(", ", winners) + ".";
        }
        queue(group, "GameNight: Voting has ended. " + outcome);
    }

    private void queue(GameGroup group, String message) {
        if (group.isDiscordWebhookConfigured()) {
            events.publishEvent(new Delivery(group.getId(), group.getDiscordWebhookUrl(), message));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(Delivery delivery) {
        try {
            client.send(delivery.webhookUrl, delivery.message);
        } catch (RuntimeException exception) {
            // Exception messages, response bodies and request URLs can contain the secret token.
            log.warn("Discord delivery failed for group {} ({})", delivery.groupId,
                    exception.getClass().getSimpleName());
        }
    }

    // No generated toString: the URL must never appear in event logging.
    public static final class Delivery {
        private final Long groupId;
        private final String webhookUrl;
        private final String message;

        private Delivery(Long groupId, String webhookUrl, String message) {
            this.groupId = groupId;
            this.webhookUrl = webhookUrl;
            this.message = message;
        }
    }
}
