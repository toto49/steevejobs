package controller.support;

import java.lang.reflect.Field;

public final class ControllerFieldInjector {

    private ControllerFieldInjector() {
    }

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
