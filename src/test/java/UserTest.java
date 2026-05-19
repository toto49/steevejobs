import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void createUser_nomVide_doitLeverException() {
        UserService service = new UserService(null);
        User user = new User(0, "", "Jean", "jean@test.fr", "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);

        // 1er type d'assertion : assertThrows
        assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
    }

    @Test
    void hashPassword_retourneChaine64Chars() {
        UserService service = new UserService(null);
        String hash = service.hashPassword("monMotDePasse");

        // 2ème type : assertNotNull
        assertNotNull(hash, "Le hash ne doit pas être null");
        // 3ème type : assertEquals
        assertEquals(64, hash.length(), "Le hachage SHA-256 doit faire 64 caractères hexadécimaux");
    }

    @Test
    void authenticate_identifiantsValides_doitRetournerUtilisateur() throws Exception {
        User expectedUser = new User(1, "Jobs", "Steve", "steve@apple.com", "hash", "", "ADMIN", "", "PDG", true);

        UserDAO fakeDAO = new UserDAO() {
            @Override public User getByEmail(String email) { return expectedUser; }
            @Override public User authenticate(String email, String passwordHash) { return expectedUser; }
        };
        UserService service = new UserService(fakeDAO);

        User result = service.authenticate("steve@apple.com", "hash");

        assertNotNull(result);
        assertEquals("Steve", result.getPrenom());
    }

    @Test
    void authenticate_compteDesactive_doitLeverSecurityException() {
        UserDAO fakeDAO = new UserDAO() {
            @Override public User getByEmail(String email) {
                return new User(1, "Jobs", "Steve", email, "hash", "", "ADMIN", "", "PDG", false); // actif = false
            }
        };
        UserService service = new UserService(fakeDAO);

        assertThrows(SecurityException.class, () -> service.authenticate("steve@apple.com", "hash"));
    }

    @Test
    void checkEmailExists_emailExistant_doitRetournerTrue() throws SQLException {
        UserDAO fakeDAO = new UserDAO() {
            @Override public boolean emailExists(String email) { return true; }
        };
        UserService service = new UserService(fakeDAO);

        // 4ème type : assertTrue
        assertTrue(service.checkEmailExists("steve@apple.com"), "Doit retourner true si l'email existe");
    }
}