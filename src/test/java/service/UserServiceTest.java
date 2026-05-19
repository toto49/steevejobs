package service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    @Test
    void createUser_emailDejaExistant_doitLeverException() throws SQLException {
        // Mock manuel du DAO : on simule que l'email existe déjà
        UserDAO fakeDAO = new UserDAO() {
            @Override public boolean emailExists(String email) { return true; }
        };
        UserService service = new UserService(fakeDAO);
        User user = new User(0, "Dupont", "Jean", "jean@test.fr", "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);

        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
    }

    @Test
    void createUser_nomVide_doitLeverException() {
        UserService service = new UserService(new UserDAO());
        // Nom vide pour déclencher l'exception avant même l'appel au DAO
        User user = new User(0, "", "Jean", "jean@test.fr", "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);

        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
    }

    @Test
    void authenticate_compteDesactive_doitLeverSecurityException() {
        UserDAO fakeDAO = new UserDAO() {
            @Override public User getByEmail(String email) {
                // Retourne un utilisateur avec actif = false
                return new User(1, "Dupont", "Jean", email, "hash", "", "EMPLOYE", "", "Dev", false);
            }
        };
        UserService service = new UserService(fakeDAO);

        assertThrows(SecurityException.class, () -> service.authenticate("jean@test.fr", "hash"));
    }

    @Test
    void hashPassword_retourneChaine64Chars() {
        UserService service = new UserService();
        String hash = service.hashPassword("monMotDePasse");

        assertEquals(64, hash.length(), "Le hachage SHA-256 doit faire 64 caractères hexadécimaux.");
    }

    @Test
    void hashPassword_memEntree_memeSortie() {
        UserService service = new UserService();
        assertEquals(service.hashPassword("steevejobs"), service.hashPassword("steevejobs"));
    }
}