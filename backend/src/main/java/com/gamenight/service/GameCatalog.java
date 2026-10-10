package com.gamenight.service;

import com.gamenight.dto.LibraryDtos.GameResponse;
import com.gamenight.model.Game;
import com.gamenight.repository.GameRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GameCatalog implements ApplicationRunner {
    private record Entry(String title, Set<String> tags, boolean multiplayer, int maxPlayers) {}

    private static final List<Entry> GAMES = List.of(
            new Entry("Among Us", Set.of("Party", "Social Deduction"), true, 15),
            new Entry("Baldur's Gate 3", Set.of("RPG", "Strategy", "Co-op"), true, 4),
            new Entry("Celeste", Set.of("Platformer", "Adventure"), false, 1),
            new Entry("Counter-Strike 2", Set.of("Shooter", "Action"), true, 10),
            new Entry("Dead by Daylight", Set.of("Horror", "Survival"), true, 5),
            new Entry("Deep Rock Galactic", Set.of("Shooter", "Co-op", "Action"), true, 4),
            new Entry("Hades", Set.of("Action", "RPG"), false, 1),
            new Entry("It Takes Two", Set.of("Adventure", "Puzzle", "Co-op"), true, 2),
            new Entry("Lethal Company", Set.of("Horror", "Survival", "Co-op"), true, 4),
            new Entry("Mario Kart 8 Deluxe", Set.of("Racing", "Party"), true, 12),
            new Entry("Minecraft", Set.of("Sandbox", "Survival", "Building"), true, 8),
            new Entry("Overcooked! 2", Set.of("Party", "Co-op", "Simulation"), true, 4),
            new Entry("Phasmophobia", Set.of("Horror", "Co-op"), true, 4),
            new Entry("Portal 2", Set.of("Puzzle", "Co-op"), true, 2),
            new Entry("Rocket League", Set.of("Sports", "Racing"), true, 8),
            new Entry("Sea of Thieves", Set.of("Adventure", "Co-op", "Action"), true, 4),
            new Entry("Slay the Spire", Set.of("Strategy", "RPG"), false, 1),
            new Entry("Stardew Valley", Set.of("Simulation", "Co-op"), true, 4),
            new Entry("Terraria", Set.of("Survival", "Sandbox", "Adventure"), true, 8),
            new Entry("Valorant", Set.of("Shooter", "Strategy"), true, 5)
    );

    private static final Set<String> TITLES = GAMES.stream()
            .map(entry -> normalize(entry.title()))
            .collect(Collectors.toUnmodifiableSet());

    private final GameRepository gameRepository;

    public GameCatalog(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        for (Entry entry : GAMES) {
            String normalizedTitle = normalize(entry.title());
            Game game = gameRepository.findByNormalizedTitle(normalizedTitle).orElse(null);
            if (game == null) {
                gameRepository.save(new Game(entry.title(), normalizedTitle,
                        entry.tags(), entry.multiplayer(), entry.maxPlayers()));
            } else if (!game.getTitle().equals(entry.title())
                    || !Set.copyOf(game.getTags()).equals(entry.tags())
                    || game.isMultiplayerSupport() != entry.multiplayer()
                    || game.getMaxPlayers() != entry.maxPlayers()) {
                game.updateCatalogMetadata(entry.title(), entry.tags(),
                        entry.multiplayer(), entry.maxPlayers());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<GameResponse> getGames() {
        return gameRepository.findAll().stream()
                .filter(game -> contains(game.getNormalizedTitle()))
                .map(game -> new GameResponse(game.getId(), game.getTitle(), game.getTags(),
                        game.isMultiplayerSupport(), game.getMaxPlayers()))
                .sorted((left, right) -> left.title().compareToIgnoreCase(right.title()))
                .toList();
    }

    public List<String> getTags() {
        return Stream.concat(GAMES.stream().flatMap(game -> game.tags().stream()),
                Stream.of("Other")).distinct().sorted().toList();
    }

    public static boolean contains(String normalizedTitle) {
        return TITLES.contains(normalizedTitle);
    }

    private static String normalize(String title) {
        return title.toLowerCase(Locale.ROOT);
    }
}
