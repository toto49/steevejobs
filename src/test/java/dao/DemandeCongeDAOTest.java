package dao;

import com.eseo.steevejobs.dao.DemandeCongeDAO;
import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.User;
import dao.support.DaoIntegrationExtension;
import dao.support.DaoTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration du {@link com.eseo.steevejobs.dao.DemandeCongeDAO}.
 * <p>
 * Vérifie création, lecture par identifiant, filtre par statut et par employé.
 * </p>
 */
@ExtendWith(DaoIntegrationExtension.class)
class DemandeCongeDAOTest {

    private UserDAO userDAO;
    private DemandeCongeDAO demandeCongeDAO;

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        demandeCongeDAO = new DemandeCongeDAO();
    }

    @Test
    void create_puisFindById_retourneDemande() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        DemandeConge demande = DaoTestFixtures.insertDemandeConge(demandeCongeDAO, employe);

        DemandeConge lu = demandeCongeDAO.findById(demande.getId());

        assertNotNull(lu);
        assertEquals(employe.getId(), lu.getEmploye().getId());
    }

    @Test
    void findByStatut_retourneDemandesEnAttente() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        DemandeConge demande = DaoTestFixtures.insertDemandeConge(demandeCongeDAO, employe);

        List<DemandeConge> demandes = demandeCongeDAO.findByStatut(StatutDemandeConge.EN_ATTENTE);

        assertTrue(demandes.stream().anyMatch(d -> d.getId() == demande.getId()));
    }

    @Test
    void findByUserId_retourneDemandesEmploye() throws SQLException {
        User employe = DaoTestFixtures.insertUser(userDAO);
        DemandeConge demande = DaoTestFixtures.insertDemandeConge(demandeCongeDAO, employe);

        List<DemandeConge> demandes = demandeCongeDAO.findByUserId(employe.getId());

        assertFalse(demandes.isEmpty());
        assertEquals(demande.getId(), demandes.get(0).getId());
    }
}
