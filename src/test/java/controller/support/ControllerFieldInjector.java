package controller.support;

import java.lang.reflect.Field;

/**
 * Utilitaire d'injection réflexive de champs {@code @FXML} ou privés sur les contrôleurs JavaFX en test.
 * <p>
 * Permet de simuler le chargement FXML sans instancier le chargeur complet.
 * </p>
 */
public final class ControllerFieldInjector {

    private ControllerFieldInjector() {
    }

    /**
     * Affecte une valeur à un champ déclaré sur le contrôleur ou une de ses superclasses.
     *
     * @param controller instance du contrôleur cible
     * @param fieldName  nom du champ à renseigner
     * @param value      valeur à injecter
     * @throws IllegalStateException si le champ est introuvable ou inaccessible en écriture
     */
    public static void inject(Object controller, String fieldName, Object value) {
        try {
            Field field = findField(controller.getClass(), fieldName);
            field.setAccessible(true);
            field.set(controller, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossible d'injecter le champ " + fieldName, e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
