import com.eseo.steevejobs.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Point d'entrée manuel pour valider la connectivité JDBC vers la base de production ou de test.
 * <p>
 * Exécution hors JUnit : aucune fixture ni extension de test n'est appliquée.
 * </p>
 */
public class DatabaseConnexion {

    /**
     * Ouvre une connexion via {@link DatabaseConnection} et affiche le résultat sur la sortie standard.
     *
     * @param arg arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] arg) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            if (connection != null) {
                System.out.println("Connexion réussie !");
            } else {
                System.out.println("La coonnexion est nulle !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la connexion : " + e.getMessage());
        }
    }
}
