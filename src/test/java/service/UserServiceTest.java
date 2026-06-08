package service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import service.support.MockitoJava25Support;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.UserService}.
 * <p>
 * Couvre création, authentification BCrypt, blocage après échecs et désactivation.
 * Mockito sur {@code UserDAO} uniquement.
 * </p>
 */
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
        String passwordClair = "monMotDePasse";
        String hash = BCrypt.hashpw(passwordClair, BCrypt.gensalt(12));
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", hash, "", "EMPLOYE", "", "Dev", true);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        User resultat = service.authenticate("jean@test.fr", passwordClair);

        assertNotNull(resultat);
        assertEquals(1, resultat.getId());
    }

    @Test
    void authenticate_motDePasseIncorrect_doitLeverSecurityException() throws Exception {
        String hash = BCrypt.hashpw("bonMotDePasse", BCrypt.gensalt(12));
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", hash, "", "EMPLOYE", "", "Dev", true);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        assertThrows(SecurityException.class, () -> service.authenticate("jean@test.fr", "mauvais"));
    }

    @Test
    void authenticate_cinqEchecs_doitBloquerLeCompte() throws Exception {
        String hash = BCrypt.hashpw("bonMotDePasse", BCrypt.gensalt(12));
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", hash, "", "EMPLOYE", "", "Dev", true);
        user.setTentativesEchouees(4);
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.authenticate("jean@test.fr", "mauvais"));

        assertTrue(ex.getMessage().contains("bloqué"));
        verify(userDAO).updateTentativesEtBlocage(eq(1), eq(5), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void authenticate_compteDejaBloque_doitRefuserLaConnexion() throws Exception {
        String hash = BCrypt.hashpw("bonMotDePasse", BCrypt.gensalt(12));
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", hash, "", "EMPLOYE", "", "Dev", true);
        user.setBloqueJusqua(LocalDateTime.now().plusMinutes(10));
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.authenticate("jean@test.fr", "bonMotDePasse"));

        assertTrue(ex.getMessage().contains("Compte bloqué"));
        verify(userDAO, never()).updateTentativesEtBlocage(anyInt(), anyInt(), any(), any());
    }

    @Test
    void authenticate_succesApresEchecs_reinitialiseLesTentatives() throws Exception {
        String passwordClair = "monMotDePasse";
        String hash = BCrypt.hashpw(passwordClair, BCrypt.gensalt(12));
        User user = new User(1, "Dupont", "Jean", "jean@test.fr", hash, "", "EMPLOYE", "", "Dev", true);
        user.setTentativesEchouees(2);
        user.setDateDernierEchec(LocalDateTime.now().minusMinutes(1));
        when(userDAO.getByEmail("jean@test.fr")).thenReturn(user);

        assertNotNull(service.authenticate("jean@test.fr", passwordClair));
        verify(userDAO).updateTentativesEtBlocage(1, 0, null, null);
    }

    @Test
    void hashPassword_retourneUnHashBCrypt() {
        String hash = service.hashPassword("monMotDePasse");
        assertTrue(hash.startsWith("$2a$"));
        assertTrue(BCrypt.checkpw("monMotDePasse", hash));
    }

    @Test
    void deactivateUser_idInvalide_doitLeverException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.deactivateUser(0));
        assertEquals("ID utilisateur invalide", ex.getMessage());
    }

}
