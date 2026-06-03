package service.support;

/**
 * Active le mode expérimental Byte Buddy avant l'initialisation de Mockito.
 * <p>
 * Nécessaire sur JDK 25 lorsque les tests sont lancés depuis IntelliJ (hors Maven Surefire).
 * Cycle de vie : la propriété {@code net.bytebuddy.experimental} est posée au chargement de la classe.
 * </p>
 */
public final class MockitoJava25Support {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private MockitoJava25Support() {
    }

    /**
     * Force le chargement de cette classe et l'exécution du bloc statique d'initialisation.
     */
    public static void enable() {
    }
}
