package dao.support;

import java.sql.SQLException;

/**
 * Action de nettoyage SQL exécutée après un test d'intégration DAO.
 */
@FunctionalInterface
public interface SqlCleanup {

    /**
     * Supprime ou annule les données insérées pendant le test.
     *
     * @throws SQLException en cas d'échec d'accès à la base
     */
    void run() throws SQLException;
}
