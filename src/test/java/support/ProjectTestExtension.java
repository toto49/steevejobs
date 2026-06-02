package support;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Active le mode test avant toute exécution JUnit (console silencieuse, pas de WS/BDD async UI).
 */
public class ProjectTestExtension implements BeforeAllCallback {

    static {
        System.setProperty("steevejobs.test", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari", "warn");
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        // propriétés déjà posées dans le bloc static
    }
}
