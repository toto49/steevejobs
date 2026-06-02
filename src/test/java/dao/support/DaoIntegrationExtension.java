package dao.support;

import com.eseo.steevejobs.dao.DatabaseConnection;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initialise une base H2 en mémoire (MODE MySQL) et nettoie les données insérées après chaque test.
 */
public class DaoIntegrationExtension implements BeforeAllCallback, AfterEachCallback, AfterAllCallback {

    private static final AtomicBoolean SCHEMA_READY = new AtomicBoolean(false);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        DatabaseConnection.reconfigureForTests(
                "jdbc:h2:mem:steevejobs_dao_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER",
                "sa",
                ""
        );
        if (SCHEMA_READY.compareAndSet(false, true)) {
            runSchemaScript();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        DaoTestCleanup.runAll();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        DatabaseConnection.restoreProductionPool();
    }

    private static void runSchemaScript() throws IOException, SQLException {
        try (InputStream in = DaoIntegrationExtension.class.getResourceAsStream("/dao/schema-h2.sql")) {
            if (in == null) {
                throw new IllegalStateException("Fichier /dao/schema-h2.sql introuvable");
            }
            String script = stripSqlComments(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String raw : script.split(";")) {
                    String sql = raw.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
        }
    }

    private static String stripSqlComments(String script) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        return cleaned.toString();
    }
}
