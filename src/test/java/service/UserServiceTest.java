package service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;
import service.support.MockitoJava25Support;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private UserDAO userDAO;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userDAO);
    }

    @Test
    void createUser_donneesValides_neDoitPasLeverException() throws SQLException {
        // Arrange
        User user = new User(0, "Dupont", "Jean", "jean@test.fr", "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);
        when(userDAO.emailExists("jean@test.fr")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> service.createUser(user));
        verify(userDAO).createUser(user);
    }

    @Test
    void createUser_emailDejaExistant_doitLeverException() throws SQLException {
        // Arrange
        User user = new User(0, "Dupont", "Jean", "jean@test.fr", "hash123", "Adresse", "EMPLOYE", "0102030405", "Dev", true);
        when(userDAO.emailExists("jean@test.fr")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
        assertEquals("Un utilisateur avec cet email existe déjà", ex.getMessage());
        verify(userDAO, never()).createUser(any());
    }

    @Test
    void createUser_motDePasseVide_doitLeverException() {
        // Arrange
        User user = new User(0, "Dupont", "Jean", "jean@test.fr", "", "Adresse", "EMPLOYE", "0102030405", "Dev", true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createUser(user));
        assertEquals("Le mot de passe est obligatoire", ex.getMessage());
    }

    @Test
    void authenticate_compteDesactive_doitLeverSecurityException() throws SQLException {
        // Arrange
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", "hash", "", "EMPLOYE", "", "Dev", false);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        // Act & Assert
        assertThrows(SecurityException.class, () -> service.authenticate("jean@test.fr", "hash"));
    }

    @Test
    void updateUser_emailUtiliseParAutreUtilisateur_doitLeverException() throws SQLException {
        // Arrange
        User user = new User(1, "Dupont", "Jean", "nouveau@test.fr", "hash", "Adresse", "EMPLOYE", "0102030405", "Dev", true);
        User autreUtilisateur = new User(2, "Martin", "Paul", "nouveau@test.fr", "hash2", "Adresse", "EMPLOYE", "0102030405", "Dev", true);

        when(userDAO.getById(1)).thenReturn(user);
        when(userDAO.getByEmail("nouveau@test.fr")).thenReturn(autreUtilisateur);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.updateUser(user));
        assertEquals("Cet email est déjà utilisé par un autre utilisateur", ex.getMessage());
        verify(userDAO, never()).updateUser(any());
    }

    @Test
    void authenticate_identifiantsCorrects_retourneUtilisateur() throws Exception {
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", "hash", "", "EMPLOYE", "", "Dev", true);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);
        when(userDAO.authenticate("jean@test.fr", "hash")).thenReturn(user);

        User resultat = service.authenticate("jean@test.fr", "hash");

        assertNotNull(resultat);
        assertEquals(1, resultat.getId());
    }

    @Test
    void authenticate_motDePasseIncorrect_doitLeverSecurityException() throws Exception {
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", "hash", "", "EMPLOYE", "", "Dev", true);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);
        when(userDAO.authenticate("jean@test.fr", "mauvais")).thenReturn(null);

        assertThrows(SecurityException.class, () -> service.authenticate("jean@test.fr", "mauvais"));
    }

    @Test
    void hashPassword_retourneSha256De64Caracteres() {
        String hash = service.hashPassword("monMotDePasse");
        assertEquals(64, hash.length());
    }

    @Test
    void hashPassword_memeEntree_memeSortie() {
        assertEquals(service.hashPassword("steevejobs"), service.hashPassword("steevejobs"));
    }

    @Test
    void deactivateUser_idInvalide_doitLeverException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.deactivateUser(0));
        assertEquals("ID utilisateur invalide", ex.getMessage());
    }

    @Test
    void activateUser_idInexistant_retourneFalse() throws SQLException {
        when(userDAO.activateUser(99)).thenReturn(false);

        assertFalse(service.activateUser(99));
    }

    @Test
    void updateUserPassword_motDePasseVide_doitLeverException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateUserPassword(1, "  "));
        assertEquals("Le mot de passe est obligatoire", ex.getMessage());
    }

    @Test
    void getIdsByRole_retourneLesIdentifiants() throws SQLException {
        when(userDAO.findByRole("RH")).thenReturn(List.of(
                new User(1, "A", "B", "a@t.fr", "h", "", "RH", "", "RH", true),
                new User(2, "C", "D", "c@t.fr", "h", "", "RH", "", "RH", true)
        ));

        assertEquals(List.of(1, 2), service.getIdsByRole("RH"));
    }
}
