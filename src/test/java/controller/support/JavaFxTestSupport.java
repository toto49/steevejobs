package controller.support;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class JavaFxTestSupport {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFxTestSupport() {
    }

    public static void ensureInitialized() {
        if (STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException ignored) {
                // JavaFX toolkit déjà démarré
            }
        }
    }

    public static void runOnFxThread(Runnable action) throws Exception {
        ensureInitialized();
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timeout en attente du thread JavaFX");
        }
        if (error.get() != null) {
            if (error.get() instanceof Exception ex) {
                throw ex;
            }
            throw new RuntimeException(error.get());
        }
    }

    public static void drainFxEvents() throws Exception {
        ensureInitialized();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timeout en attente des événements JavaFX");
        }
    }
}
