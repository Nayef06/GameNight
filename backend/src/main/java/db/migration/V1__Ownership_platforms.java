package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Runs before Hibernate updates the existing schema, preserving every ownership row. */
public class V1__Ownership_platforms extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String schema = connection.getSchema();
        String table = null;
        try (var tables = connection.getMetaData().getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                if ("user_games".equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    table = tables.getString("TABLE_NAME");
                    break;
                }
            }
        }
        // On fresh databases Hibernate creates the table using the new mapping.
        if (table == null) return;
        String qualifiedTable = quote(schema) + "." + quote(table);
        try (var statement = connection.createStatement()) {
            statement.execute("alter table " + qualifiedTable
                    + " add column platform varchar(20) default 'OTHER' not null");
            statement.execute("alter table " + qualifiedTable
                    + " add column platform_unconfirmed boolean default true not null");
            statement.execute("alter table " + qualifiedTable
                    + " alter column platform_unconfirmed set default false");
        }

        Map<String, Set<String>> uniqueConstraints = new HashMap<>();
        try (var query = connection.prepareStatement("""
                select tc.constraint_name, kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_catalog = kcu.constraint_catalog
                 and tc.constraint_schema = kcu.constraint_schema
                 and tc.constraint_name = kcu.constraint_name
                where tc.table_schema = ? and tc.table_name = ? and tc.constraint_type = 'UNIQUE'
                """)) {
            query.setString(1, schema);
            query.setString(2, table);
            try (var rows = query.executeQuery()) {
                while (rows.next()) {
                    uniqueConstraints.computeIfAbsent(rows.getString(1), ignored -> new HashSet<>())
                            .add(rows.getString(2).toLowerCase(Locale.ROOT));
                }
            }
        }
        try (var statement = connection.createStatement()) {
            statement.execute("alter table " + qualifiedTable
                    + " add constraint uk_user_game_platform unique (user_id, game_id, platform)");
            for (var constraint : uniqueConstraints.entrySet()) {
                if (constraint.getValue().equals(Set.of("user_id", "game_id"))) {
                    statement.execute("alter table " + qualifiedTable + " drop constraint "
                            + quote(constraint.getKey()));
                }
            }
        }
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
