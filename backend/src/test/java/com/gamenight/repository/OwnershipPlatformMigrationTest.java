package com.gamenight.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipPlatformMigrationTest {
    @Test
    void migratesOldOwnershipsPreservesRowsAndReplacesPairUniqueness() throws Exception {
        String url = "jdbc:h2:mem:oldownerships;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("create table user_games (id bigint primary key, user_id bigint not null, "
                    + "game_id bigint not null, constraint old_ownership_unique unique (user_id, game_id))");
            statement.execute("insert into user_games (id, user_id, game_id) values (11, 1, 2), (12, 2, 2)");
        }
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "")
                .baselineOnMigrate(true).baselineVersion("0").load();
        flyway.migrate();
        flyway.migrate(); // Restarting does not repeat the migration.
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("select * from user_games order by id")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(11);
                assertThat(rows.getString("platform")).isEqualTo("OTHER");
                assertThat(rows.getBoolean("platform_unconfirmed")).isTrue();
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("id")).isEqualTo(12);
            }
            statement.execute("insert into user_games (id, user_id, game_id, platform) values (13, 1, 2, 'STEAM')");
            try (var rows = statement.executeQuery("select platform_unconfirmed from user_games where id = 13")) {
                rows.next();
                assertThat(rows.getBoolean(1)).isFalse();
            }
            assertThatThrownBy(() -> statement.execute("insert into user_games (id, user_id, game_id, platform) "
                    + "values (14, 1, 2, 'STEAM')")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("insert into user_games (id, user_id, game_id, platform) "
                    + "values (15, 1, 2, null)")).isInstanceOf(SQLException.class);
        }
    }
}
