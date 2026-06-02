package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.VisioDAO;
import com.eseo.steevejobs.model.Enum.ReunionType;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.SalonAccesInfo;
import com.eseo.steevejobs.model.SalonEnCoursInfo;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.util.TestRuntime;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisioService {

    private static final ZoneId FUSEAU_HORAIRE = ZoneId.systemDefault();

    private final VisioDAO visioDAO;

    public VisioService() {
        this.visioDAO = new VisioDAO();
    }

    public VisioService(VisioDAO visioDAO) {
        this.visioDAO = visioDAO;
    }

    public Optional<String> validerNomSalon(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return Optional.of("Nom de salle obligatoire.");
        }
        return Optional.empty();
    }

    /**
     * @return message d'erreur si l'heure est invalide, vide si OK
     */
    public Optional<String> validerHeureProgrammee(LocalDateTime heureProg) {
        if (heureProg == null) {
            return Optional.of("Date et heure obligatoires.");
        }
        if (heureProg.isBefore(LocalDateTime.now(FUSEAU_HORAIRE))) {
            return Optional.of("Impossible de planifier une réunion dans le passé.");
        }
        return Optional.empty();
    }

    public Optional<String> validerPlanification(String roomName, LocalDateTime heureProg) {
        Optional<String> err = validerNomSalon(roomName);
        if (err.isPresent()) {
            return err;
        }
        return validerHeureProgrammee(heureProg);
    }

    private void activerSalonsPlanifiesEligibles() {
        int misAJour = visioDAO.activerSalonsPlanifiesEligibles(LocalDateTime.now(FUSEAU_HORAIRE));
        if (misAJour > 0 && !TestRuntime.isEnabled()) {
            System.out.println("✅ " + misAJour + " réunion(s) planifiée(s) passée(s) en EN_COURS.");
        }
    }

    private boolean estSalonInstantane(String typeReunion, LocalDateTime heureProgrammee) {
        return ReunionType.INSTANTANEE.name().equals(typeReunion)
                || (typeReunion == null && heureProgrammee == null);
    }

    private boolean estAccesAutorise(SalonAccesInfo info, int userId) {
        if (info.type() == ReunionType.INSTANTANEE) {
            return true;
        }
        if (info.statut() == VisioStatut.EN_COURS && info.heureProgrammee() == null) {
            return true;
        }
        return userId == info.createurId() || info.invite();
    }

    public JSONObject traiterDemandeConnexion(String roomName, int userId, String userName) {
        JSONObject reponse = new JSONObject();

        Optional<String> errNom = validerNomSalon(roomName);
        if (errNom.isPresent()) {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "⚠️ Le nom de la salle est invalide.");
            return reponse;
        }

        roomName = roomName.trim();
        activerSalonsPlanifiesEligibles();

        if (!visioDAO.existeEnBdd(roomName)) {
            if (!TestRuntime.isEnabled()) {
                System.out.println("✨ Initialisation d'une visioconférence instantanée publique : " + roomName);
            }
            Visio instantAppel = new Visio(roomName, userId, null);
            instantAppel.setStatut(VisioStatut.EN_COURS);
            instantAppel.setType_reunion(ReunionType.INSTANTANEE);
            visioDAO.enregistrerSalonInstantane(instantAppel);
        }

        Optional<SalonAccesInfo> infos = visioDAO.chargerInfosAccesSalon(roomName, userId);
        boolean accesOk = infos.isPresent() && estAccesAutorise(infos.get(), userId);

        if (accesOk) {
            visioDAO.ouvrirSalon(roomName);
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "SUCCESS");
            reponse.put("roomName", roomName);
            if (!TestRuntime.isEnabled()) {
                System.out.println("✅ Autorisation d'accès BDD accordée pour " + userName + " sur le salon [" + roomName + "]");
            }
        } else {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "❌ Accès refusé : Vous ne figurez pas sur le registre des invités de cette session.");
        }

        return reponse;
    }

    public boolean planifierNouvelleReunion(String roomName, int createurId, LocalDateTime heureProg, List<Integer> idInvites) {
        if (validerPlanification(roomName, heureProg).isPresent()) {
            return false;
        }

        Visio planification = new Visio(roomName.trim(), createurId, heureProg);
        planification.setStatut(VisioStatut.PROGRAMMEE);
        boolean ok = visioDAO.planifierReunion(planification, idInvites);
        if (ok) {
            activerSalonsPlanifiesEligibles();
        }
        return ok;
    }

    public List<Visio> obtenirReunionsAccessibles(int userId) {
        if (userId <= 0) {
            return new ArrayList<>();
        }
        activerSalonsPlanifiesEligibles();
        return visioDAO.listerReunionsDisponibles(userId);
    }

    public void couperSalonDefinitif(String roomName, int userId) {
        Optional<String> errNom = validerNomSalon(roomName);
        if (errNom.isPresent() || !visioDAO.isCreateur(roomName.trim(), userId)) {
            return;
        }

        String nom = roomName.trim();
        Optional<SalonEnCoursInfo> enCours = visioDAO.chargerSalonEnCours(nom);

        if (enCours.isEmpty() || estSalonInstantane(enCours.get().typeReunion(), enCours.get().heureProgrammee())) {
            visioDAO.supprimerSalonInstantane(nom);
        } else {
            visioDAO.terminerSalonPlanifie(nom);
        }
        if (!TestRuntime.isEnabled()) {
            System.out.println("🔒 Salon clôturé : " + nom);
        }
    }
}
