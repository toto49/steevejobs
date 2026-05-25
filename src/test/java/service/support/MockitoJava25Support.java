package service.support;

/**
 * Force l'activation du mode expérimental Byte Buddy avant l'initialisation de Mockito.
 * Necessaire sur JDK 25 lorsque les tests sont lances depuis IntelliJ (hors Maven Surefire).
 */
public final class MockitoJava25Support {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private MockitoJava25Support() {
    }

    public static void enable() {
        // Declenche le chargement de cette classe et donc le bloc static ci-dessus.
    }
}
