package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.util.TestRuntime;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dotenv.get("DB_URL"));
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

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Réinitialise le pool JDBC pour les tests d'intégration DAO (H2 en mémoire).
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
     * Restaure le pool MySQL de production après les tests DAO (évite les effets de bord sur les tests UI).
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

    public static void fermerPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}