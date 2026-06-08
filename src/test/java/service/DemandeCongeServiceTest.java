package service;

import com.eseo.steevejobs.dao.DemandeCongeDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.DemandeConge;
import com.eseo.steevejobs.model.Enum.StatutDemandeConge;
import com.eseo.steevejobs.model.Planning;
import com.eseo.steevejobs.model.SoldeConge;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.CongeUtil;
import com.eseo.steevejobs.service.DemandeCongeService;
import com.eseo.steevejobs.service.PlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.TestDataFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.DemandeCongeService} avec DAO et planning mockés.
 * <p>
 * Cycle de vie : {@code @BeforeEach} instancie le service ; fixtures {@link service.support.TestDataFactory}.
 * Couvre création, validation, refus, solde et suppression de congés validés.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class DemandeCongeServiceTest {

    @Mock
    private DemandeCongeDAO demandeCongeDAO;
    @Mock
    private PlanningDAO planningDAO;
    @Mock
    private PlanningService planningService;

    private DemandeCongeService service;

    @BeforeEach
    void setUp() {
        service = new DemandeCongeService(demandeCongeDAO, planningDAO, planningService);
    }

    @Test
    void creerDemande_soldeSuffisant_enregistreLaDemande() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDate premierJour = LocalDate.now().plusDays(1);
        LocalDateTime debut = LocalDateTime.of(premierJour, LocalTime.of(8, 0));
        LocalDateTime fin = LocalDateTime.of(premierJour.plusDays(2), LocalTime.of(18, 0));

        when(planningDAO.findByUserId(1)).thenReturn(List.of());
        when(demandeCongeDAO.findByUserId(1)).thenReturn(List.of());
        when(demandeCongeDAO.create(any(DemandeConge.class))).thenReturn(true);

        DemandeConge demande = service.creerDemande(employe, debut, fin, "Vacances familiales");

        assertEquals(StatutDemandeConge.EN_ATTENTE, demande.getStatut());
        verify(demandeCongeDAO).create(any(DemandeConge.class));
    }

    @Test
    void creerDemande_soldeInsuffisant_doitLeverException() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDate debut = LocalDate.now().plusDays(10);
        LocalDateTime start = LocalDateTime.of(debut, java.time.LocalTime.of(8, 0));
        LocalDateTime end = LocalDateTime.of(debut.plusDays(30), java.time.LocalTime.of(18, 0));

        when(planningDAO.findByUserId(1)).thenReturn(List.of());
        when(demandeCongeDAO.findByUserId(1)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.creerDemande(employe, start, end, "Trop long"));
    }

    @Test
    void validerDemande_enAttente_creeUnPlanning() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(2, "rh@mail.fr");
        DemandeConge demande = new DemandeConge(
                5,
                LocalDateTime.of(2026, 7, 7, 8, 0),
                LocalDateTime.of(2026, 7, 8, 18, 0),
                StatutDemandeConge.EN_ATTENTE,
                "Repos",
                null,
                LocalDateTime.now(),
                employe,
                0
        );

        when(demandeCongeDAO.findById(5)).thenReturn(demande);
        when(planningDAO.findByUserId(2)).thenReturn(List.of());
        when(demandeCongeDAO.findByUserId(2)).thenReturn(List.of(demande));
        doAnswer(invocation -> {
            Planning p = invocation.getArgument(0);
            p.setId(99);
            return null;
        }).when(planningService).ajouterPlanning(any(Planning.class));
        when(demandeCongeDAO.update(any(DemandeConge.class))).thenReturn(true);

        service.validerDemande(5, "Validé");

        verify(planningService).ajouterPlanning(any(Planning.class));
        verify(demandeCongeDAO).update(argThat(d ->
                d.getStatut() == StatutDemandeConge.VALIDEE && d.getIdPlanning() == 99));
    }

    @Test
    void calculerSoldeConge_retourneLesBonnesValeurs() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(3, "emp@mail.fr");
        Planning conge = new Planning(
                1,
                LocalDateTime.of(2026, 3, 10, 8, 0),
                LocalDateTime.of(2026, 3, 11, 18, 0),
                CongeUtil.TYPE_CONGE,
                "Validé",
                CongeUtil.COULEUR_CONGE,
                employe
        );
        DemandeConge enAttente = new DemandeConge(
                2,
                LocalDateTime.of(2026, 8, 1, 8, 0),
                LocalDateTime.of(2026, 8, 2, 18, 0),
                StatutDemandeConge.EN_ATTENTE,
                "",
                null,
                LocalDateTime.now(),
                employe,
                0
        );

        when(planningDAO.findByUserId(3)).thenReturn(List.of(conge));
        when(demandeCongeDAO.findByUserId(3)).thenReturn(List.of(enAttente));

        SoldeConge solde = service.calculerSoldeConge(3, 2026);

        assertEquals(CongeUtil.JOURS_CONGE_ANNUELS, solde.getJoursAcquis());
        assertEquals(2, solde.getJoursPris());
        assertEquals(2, solde.getJoursEnAttente());
        assertEquals(21, solde.getJoursRestants());
    }

    @Test
    void modifierDemandeConge_validee_metAJourDemandeEtPlanning() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(4, "emp2@mail.fr");
        DemandeConge demande = new DemandeConge(
                10,
                LocalDateTime.of(2026, 9, 2, 8, 0),
                LocalDateTime.of(2026, 9, 3, 18, 0),
                StatutDemandeConge.VALIDEE,
                "Famille",
                null,
                LocalDateTime.now(),
                employe,
                42
        );
        Planning planning = new Planning(
                42,
                demande.getJourDebut(),
                demande.getJourFin(),
                CongeUtil.TYPE_CONGE,
                "Famille",
                CongeUtil.COULEUR_CONGE,
                employe
        );

        when(demandeCongeDAO.findById(10)).thenReturn(demande);
        when(planningDAO.getById(42)).thenReturn(planning);
        when(planningDAO.findByUserId(4)).thenReturn(List.of(planning));
        when(demandeCongeDAO.findByUserId(4)).thenReturn(List.of(demande));
        when(demandeCongeDAO.update(any(DemandeConge.class))).thenReturn(true);

        LocalDate nouveauDebut = LocalDate.of(2026, 9, 5);
        LocalDate nouvelleFin = LocalDate.of(2026, 9, 6);
        service.modifierDemandeConge(10, nouveauDebut, nouvelleFin);

        verify(planningService).modifierPlanning(argThat(p ->
                p.getId() == 42
                        && p.getJourDebut().equals(DemandeCongeService.debutJournee(nouveauDebut))
                        && p.getJourFin().equals(DemandeCongeService.finJournee(nouvelleFin))));
        verify(demandeCongeDAO).update(argThat(d ->
                d.getJourDebut().equals(DemandeCongeService.debutJournee(nouveauDebut))
                        && d.getJourFin().equals(DemandeCongeService.finJournee(nouvelleFin))));
    }

    @Test
    void supprimerCongeValide_supprimePlanningEtDemande() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(5, "emp3@mail.fr");
        DemandeConge demande = new DemandeConge(
                11,
                LocalDateTime.of(2026, 10, 1, 8, 0),
                LocalDateTime.of(2026, 10, 2, 18, 0),
                StatutDemandeConge.VALIDEE,
                "",
                null,
                LocalDateTime.now(),
                employe,
                55
        );
        Planning planning = new Planning(
                55,
                demande.getJourDebut(),
                demande.getJourFin(),
                CongeUtil.TYPE_CONGE,
                "Congés",
                CongeUtil.COULEUR_CONGE,
                employe
        );

        when(demandeCongeDAO.findById(11)).thenReturn(demande);
        when(planningDAO.getById(55)).thenReturn(planning);
        when(demandeCongeDAO.delete(11)).thenReturn(true);

        service.supprimerCongeValide(11);

        verify(planningService).supprimerPlanning(55);
        verify(demandeCongeDAO).delete(11);
    }

    @Test
    void creerDemande_dateDansLePasse_doitLeverException() {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDateTime debut = LocalDateTime.now().minusDays(2);
        LocalDateTime fin = LocalDateTime.now().minusDays(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.creerDemande(employe, debut, fin, "Passé"));
        assertTrue(ex.getMessage().contains("passé"));
    }

    @Test
    void refuserDemande_enAttente_passeEnRefusee() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(6, "emp6@mail.fr");
        DemandeConge demande = new DemandeConge(
                20,
                LocalDateTime.of(2026, 11, 1, 8, 0),
                LocalDateTime.of(2026, 11, 2, 18, 0),
                StatutDemandeConge.EN_ATTENTE,
                "Perso",
                null,
                LocalDateTime.now(),
                employe,
                0
        );

        when(demandeCongeDAO.findById(20)).thenReturn(demande);
        when(demandeCongeDAO.update(any(DemandeConge.class))).thenReturn(true);

        service.refuserDemande(20);

        verify(demandeCongeDAO).update(argThat(d -> d.getStatut() == StatutDemandeConge.REFUSEE));
        verify(planningService, never()).ajouterPlanning(any());
    }

    @Test
    void validerDemande_introuvable_doitLeverException() throws SQLException {
        when(demandeCongeDAO.findById(999)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.validerDemande(999, "OK"));
    }

}
