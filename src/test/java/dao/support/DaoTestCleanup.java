package dao.support;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Registre LIFO de suppressions à exécuter après chaque test DAO.
 */
public final class DaoTestCleanup {

    private static final Deque<SqlCleanup> CLEANUPS = new ArrayDeque<>();

    private DaoTestCleanup() {
    }

    public static void register(SqlCleanup cleanup) {
        CLEANUPS.addFirst(cleanup);
    }

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

    public static boolean isEmpty() {
        return CLEANUPS.isEmpty();
    }
}
