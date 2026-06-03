package dao.support;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Registre LIFO de suppressions SQL à exécuter après chaque test DAO via {@link DaoIntegrationExtension}.
 * <p>
 * Les fixtures {@link DaoTestFixtures} enregistrent automatiquement un nettoyage à chaque insertion.
 * </p>
 */
public final class DaoTestCleanup {

    private static final Deque<SqlCleanup> CLEANUPS = new ArrayDeque<>();

    private DaoTestCleanup() {
    }

    /**
     * Ajoute un nettoyage en tête de file (exécuté en premier lors du prochain {@link #runAll()}).
     *
     * @param cleanup action de suppression ou d'annulation
     */
    public static void register(SqlCleanup cleanup) {
        CLEANUPS.addFirst(cleanup);
    }

    /**
     * Exécute tous les nettoyages jusqu'à épuisement de la file ; la première {@link SQLException} est propagée.
     *
     * @throws SQLException en cas d'échec SQL non récupéré
     */
    public static void runAll() throws SQLException {
        SQLException deferred = null;
        while (!CLEANUPS.isEmpty()) {
            try {
                CLEANUPS.removeFirst().run();
            } catch (SQLException e) {
                deferred = e;
            }
        }
        if (deferred != null) {
            throw deferred;
        }
    }

    /**
     * Indique si aucun nettoyage n'est en attente.
     *
     * @return {@code true} si la file est vide
     */
    public static boolean isEmpty() {
        return CLEANUPS.isEmpty();
    }
}
