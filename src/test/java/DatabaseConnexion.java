import com.eseo.steevejobs.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnexion {
    public static void main(String[] arg ) {
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
