package com.eseo.steevejobs.util;

/**
 * Indique si le code s'exécute dans une suite de tests (JUnit).
 * Activé via {@code -Dsteevejobs.test=true} ou l'extension {@code ProjectTestExtension}.
 */
public final class TestRuntime {

    private TestRuntime() {
    }

    public static boolean isEnabled() {
        return Boolean.getBoolean("steevejobs.test");
    }
}
