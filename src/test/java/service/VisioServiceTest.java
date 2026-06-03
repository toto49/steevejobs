package service;

import com.eseo.steevejobs.dao.VisioDAO;
import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.SalonAccesInfo;
import com.eseo.steevejobs.model.SalonEnCoursInfo;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.service.VisioService;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.VisioService}.
 * <p>
 * Scénarios connexion salon, planification, listes et coupure définitive (instantané / planifié).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class VisioServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private VisioDAO visioDAO;

    private VisioService service;

    @BeforeEach
    void setUp() {
        service = new VisioService(visioDAO);
    }

    @Test
    void validerNomSalon_vide_retourneErreur() {
        assertTrue(service.validerNomSalon(null).isPresent());
        assertTrue(service.validerNomSalon("   ").isPresent());
    }

    @Test
    void validerNomSalon_valide_retourneVide() {
        assertTrue(service.validerNomSalon("Salle_RH").isEmpty());
    }

    @Test
    void validerHeureProgrammee_dansLePasse_retourneErreur() {
        LocalDateTime passe = LocalDateTime.now().minusDays(1);
        assertTrue(service.validerHeureProgrammee(passe).isPresent());
    }

    @Test
    void traiterDemandeConnexion_nomInvalide_retourneErreur() {
        JSONObject reponse = service.traiterDemandeConnexion("", 1, "Jean");

        assertEquals("ERROR", reponse.getString("status"));
        assertTrue(reponse.getString("message").contains("invalide"));
        verifyNoInteractions(visioDAO);
    }

    @Test
    void traiterDemandeConnexion_salonInstantaneNouveau_autoriseAcces() {
        when(visioDAO.activerSalonsPlanifiesEligibles(any())).thenReturn(0);
        when(visioDAO.existeEnBdd("Salle_Test")).thenReturn(false);
        when(visioDAO.enregistrerSalonInstantane(any(Visio.class))).thenReturn(true);
        when(visioDAO.chargerInfosAccesSalon("Salle_Test", 1)).thenReturn(Optional.of(
                new SalonAccesInfo(VisioStatut.EN_COURS, ReunionType.INSTANTANEE, 1, false, null)
        ));

        JSONObject reponse = service.traiterDemandeConnexion("Salle_Test", 1, "Jean");

        assertEquals("SUCCESS", reponse.getString("status"));
        assertEquals("Salle_Test", reponse.getString("roomName"));
        verify(visioDAO).enregistrerSalonInstantane(any(Visio.class));
        verify(visioDAO).ouvrirSalon("Salle_Test");
    }

    @Test
    void traiterDemandeConnexion_planifieeNonInvite_refuseAcces() {
        when(visioDAO.activerSalonsPlanifiesEligibles(any())).thenReturn(0);
        when(visioDAO.existeEnBdd("macaron")).thenReturn(true);
        when(visioDAO.chargerInfosAccesSalon("macaron", 5)).thenReturn(Optional.of(
                new SalonAccesInfo(
                        VisioStatut.PROGRAMMEE,
                        ReunionType.PLANIFIEE,
                        4,
                        false,
                        LocalDateTime.now().plusDays(1)
                )
        ));

        JSONObject reponse = service.traiterDemandeConnexion("macaron", 5, "Invite");

        assertEquals("ERROR", reponse.getString("status"));
        verify(visioDAO, never()).ouvrirSalon(anyString());
    }

    @Test
    void planifierNouvelleReunion_donneesInvalides_retourneFalse() {
        assertFalse(service.planifierNouvelleReunion("", 1, LocalDateTime.now().plusDays(1), List.of()));
        assertFalse(service.planifierNouvelleReunion("Salle_OK", 1, LocalDateTime.now().minusDays(1), List.of()));
        verify(visioDAO, never()).planifierReunion(any(), any());
    }

    @Test
    void planifierNouvelleReunion_donneesValides_retourneTrue() {
        LocalDateTime heure = LocalDateTime.now().plusDays(3);
        when(visioDAO.planifierReunion(any(Visio.class), eq(List.of(2, 3)))).thenReturn(true);
        when(visioDAO.activerSalonsPlanifiesEligibles(any())).thenReturn(0);

        boolean ok = service.planifierNouvelleReunion("Reunion_RH", 1, heure, List.of(2, 3));

        assertTrue(ok);
        verify(visioDAO).planifierReunion(argThat(v ->
                v.getRoom_name().equals("Reunion_RH")
                        && v.getStatut() == VisioStatut.PROGRAMMEE), eq(List.of(2, 3)));
    }

    @Test
    void obtenirReunionsAccessibles_userIdInvalide_retourneListeVide() {
        assertTrue(service.obtenirReunionsAccessibles(0).isEmpty());
        verify(visioDAO, never()).listerReunionsDisponibles(anyInt());
    }

    @Test
    void couperSalonDefinitif_salonInstantaneCreateur_supprimeEnBdd() {
        when(visioDAO.isCreateur("Salle_RH", 1)).thenReturn(true);
        when(visioDAO.chargerSalonEnCours("Salle_RH")).thenReturn(Optional.of(
                new SalonEnCoursInfo(ReunionType.INSTANTANEE.name(), null)
        ));

        service.couperSalonDefinitif("Salle_RH", 1);

        verify(visioDAO).supprimerSalonInstantane("Salle_RH");
        verify(visioDAO, never()).terminerSalonPlanifie(anyString());
    }

    @Test
    void couperSalonDefinitif_salonPlanifieCreateur_termineEnBdd() {
        when(visioDAO.isCreateur("Reunion_RH", 1)).thenReturn(true);
        when(visioDAO.chargerSalonEnCours("Reunion_RH")).thenReturn(Optional.of(
                new SalonEnCoursInfo(ReunionType.PLANIFIEE.name(), LocalDateTime.now().plusHours(1))
        ));

        service.couperSalonDefinitif("Reunion_RH", 1);

        verify(visioDAO).terminerSalonPlanifie("Reunion_RH");
        verify(visioDAO, never()).supprimerSalonInstantane(anyString());
    }

}
