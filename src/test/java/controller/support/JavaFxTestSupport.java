package controller.support;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Initialisation du toolkit JavaFX et exécution synchrone d'actions sur le thread FX pour les tests unitaires.
 * <p>
 * Cycle de vie : {@link #ensureInitialized()} démarre le toolkit une seule fois (idempotent) ;
 * les appels suivants s'exécutent via {@link Platform#runLater(Runnable)} avec attente limitée à 15 secondes.
 * </p>
 */
public final class JavaFxTestSupport {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private JavaFxTestSupport() {
    }

    /**
     * Démarre le toolkit JavaFX si ce n'est pas déjà fait (ignore {@link IllegalStateException} si déjà actif).
     */
    public static void ensureInitialized() {
        if (STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException ignored) {
            }
        }
    }

    /**
     * Exécute une action sur le thread d'application JavaFX et propage toute exception levée.
     *
     * @param action code à exécuter sur le thread FX
     * @throws Exception           exception métier remontée depuis l'action
     * @throws IllegalStateException en cas de dépassement du délai d'attente (15 s)
     */
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

    /**
     * Vide la file d'événements JavaFX en planifiant puis en attendant un runnable vide.
     *
     * @throws Exception           exception remontée depuis le thread FX
     * @throws IllegalStateException en cas de timeout
     */
    public static void drainFxEvents() throws Exception {
        ensureInitialized();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        if (!latch.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timeout en attente des événements JavaFX");
        }
    }
}
