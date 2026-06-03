package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.util.TestRuntime;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestionnaire centralisé du pool de connexions JDBC (HikariCP).
 * <p>
 * Le pool est initialisé au chargement de la classe à partir des variables d'environnement
 * {@code DB_URL}, {@code DB_USER} et {@code DB_PASSWORD}. Les DAO obtiennent une connexion
 * par appel à {@link #getConnection()} ; chaque opération SQL s'exécute en auto-commit
 * sauf gestion explicite de transaction dans le DAO appelant.
 * </p>
 * <p>
 * En cas d'échec d'initialisation, le pool reste {@code null} et {@link #getConnection()}
 * lève une {@link SQLException}. Les méthodes de reconfiguration pour les tests ferment
 * le pool existant avant d'en instancier un nouveau.
 * </p>
 */
public class DatabaseConnection {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static HikariDataSource dataSource;

    static {
        String dbUrl = dotenv.get("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            if (TestRuntime.isEnabled()) {
                return;
            }
        }
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dotenv.get("DB_USER"));
            config.setPassword(dotenv.get("DB_PASSWORD"));

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            System.err.println("❌ Erreur critique lors de l'initialisation de la BDD : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fournit une connexion JDBC issue du pool HikariCP.
     * <p>
     * La connexion doit être fermée par l'appelant (try-with-resources recommandé).
     * Aucune transaction n'est ouverte implicitement ; l'auto-commit est activé par défaut.
     * </p>
     *
     * @return connexion JDBC active
     * @throws SQLException si le pool est indisponible ou si l'obtention de connexion échoue
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Pool JDBC non initialisé (variables DB_* absentes ou mode test sans reconfigureForTests).");
        }
        return dataSource.getConnection();
    }

    /**
     * Réinitialise le pool JDBC pour les tests d'intégration DAO (H2 en mémoire).
     * <p>
     * Ferme le pool courant puis crée un nouveau pool pointant vers la base H2 fournie.
     * Opération synchronisée pour éviter les accès concurrents pendant la bascule.
     * </p>
     *
     * @param jdbcUrl  URL JDBC de la base de test
     * @param username identifiant de connexion
     * @param password mot de passe de connexion
     */
    public static synchronized void reconfigureForTests(String jdbcUrl, String username, String password) {
        fermerPool();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setDriverClassName("org.h2.Driver");
        dataSource = new HikariDataSource(config);
    }

    /**
     * Restaure le pool MySQL de production après les tests DAO.
     * <p>
     * N'exécute aucune action si le mode test n'est pas actif ou si {@code DB_URL} est absent.
     * Les erreurs de reconfiguration sont journalisées sur {@code System.err} sans relancer d'exception.
     * </p>
     */
    public static synchronized void restoreProductionPool() {
        if (!TestRuntime.isEnabled()) {
            return;
        }
        String dbUrl = dotenv.get("DB_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }
        fermerPool();
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dotenv.get("DB_USER"));
            config.setPassword(dotenv.get("DB_PASSWORD"));
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de restaurer le pool MySQL après les tests DAO : " + e.getMessage());
        }
    }

    /**
     * Ferme le pool de connexions s'il est ouvert.
     * <p>
     * Idempotent : aucune action si le pool est déjà fermé ou {@code null}.
     * </p>
     */
    public static void fermerPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}