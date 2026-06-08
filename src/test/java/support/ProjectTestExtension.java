package support;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension JUnit globale activant le mode test de l'application avant toute suite.
 * <p>
 * Cycle de vie : propriétés système posées au chargement de la classe (bloc {@code static}),
 * puis rappel vide dans {@link #beforeAll(ExtensionContext)}.
 * Effets : mode {@code steevejobs.test}, journalisation SLF4J/Hikari réduite au niveau {@code warn}.
 * </p>
 */
public class ProjectTestExtension implements BeforeAllCallback {

    static {
        System.setProperty("steevejobs.test", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari", "warn");
    }

    /**
     * Callback JUnit exécuté une fois par classe de test ; les propriétés sont déjà initialisées.
     *
     * @param context contexte d'extension JUnit (non utilisé)
     */
    @Override
    public void beforeAll(ExtensionContext context) {
    }
}
