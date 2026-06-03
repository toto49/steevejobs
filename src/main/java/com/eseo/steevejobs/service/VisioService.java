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

/**
 * Logique métier des salons de visioconférence (instantanés et planifiés).
 * <p>
 * Règles métier : réunions planifiées non passées ; accès réservé au créateur
 * et aux invités sauf salons instantanés publics ; activation automatique des
 * salons planifiés dont l'heure est échue. Retour JSON pour intégration WebSocket
 * ({@code VISIO_TOKEN_RESPONSE}). Persistance BDD uniquement, pas d'envoi média direct.
 * </p>
 */
public class VisioService {

    /** Fuseau horaire local utilisé pour les comparaisons de dates de réunion. */
    private static final ZoneId FUSEAU_HORAIRE = ZoneId.systemDefault();

    /** Accès persistance aux salons de visioconférence. */
    private final VisioDAO visioDAO;

    /**
     * Constructeur par défaut.
     */
    public VisioService() {
        this.visioDAO = new VisioDAO();
    }

    /**
     * Constructeur avec injection du DAO.
     *
     * @param visioDAO accès persistance visio
     */
    public VisioService(VisioDAO visioDAO) {
        this.visioDAO = visioDAO;
    }

    /**
     * Valide le nom de salon (non vide après trim).
     *
     * @param roomName nom de salle proposé
     * @return message d'erreur si invalide, {@link Optional#empty()} si valide
     */
    public Optional<String> validerNomSalon(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return Optional.of("Nom de salle obligatoire.");
        }
        return Optional.empty();
    }

    /**
     * Valide une date-heure de planification (présente et non passée).
     *
     * @param heureProg date-heure programmée
     * @return message d'erreur si invalide, {@link Optional#empty()} si valide
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

    /**
     * Valide nom de salon et horaire de planification conjointement.
     *
     * @param roomName  nom de salle
     * @param heureProg date-heure programmée
     * @return première erreur rencontrée, ou vide si tout est valide
     */
    public Optional<String> validerPlanification(String roomName, LocalDateTime heureProg) {
        Optional<String> err = validerNomSalon(roomName);
        if (err.isPresent()) {
            return err;
        }
        return validerHeureProgrammee(heureProg);
    }

    /**
     * Passe en statut {@link VisioStatut#EN_COURS} les réunions planifiées dont l'heure est échue.
     * <p>
     * Journalise le nombre de salons activés sauf en mode test ({@link TestRuntime}).
     * </p>
     */
    private void activerSalonsPlanifiesEligibles() {
        int misAJour = visioDAO.activerSalonsPlanifiesEligibles(LocalDateTime.now(FUSEAU_HORAIRE));
        if (misAJour > 0 && !TestRuntime.isEnabled()) {
            System.out.println("✅ " + misAJour + " réunion(s) planifiée(s) passée(s) en EN_COURS.");
        }
    }

    /**
     * Indique si un salon correspond à une visioconférence instantanée.
     *
     * @param typeReunion       libellé de type de réunion en base
     * @param heureProgrammee   date-heure planifiée, ou {@code null} pour un salon immédiat
     * @return {@code true} si le salon est de type instantané ou sans horaire programmé
     */
    private boolean estSalonInstantane(String typeReunion, LocalDateTime heureProgrammee) {
        return ReunionType.INSTANTANEE.name().equals(typeReunion)
                || (typeReunion == null && heureProgrammee == null);
    }

    /**
     * Détermine si un utilisateur est autorisé à rejoindre un salon.
     * <p>
     * Les salons instantanés et certains salons en cours sans planification sont publics ;
     * les réunions planifiées sont réservées au créateur et aux invités enregistrés.
     * </p>
     *
     * @param info   métadonnées d'accès du salon
     * @param userId identifiant de l'utilisateur demandeur
     * @return {@code true} si l'accès est accordé
     */
    private boolean estAccesAutorise(SalonAccesInfo info, int userId) {
        if (info.type() == ReunionType.INSTANTANEE) {
            return true;
        }
        if (info.statut() == VisioStatut.EN_COURS && info.heureProgrammee() == null) {
            return true;
        }
        return userId == info.createurId() || info.invite();
    }

    /**
     * Traite une demande de connexion à un salon et produit la réponse JSON WebSocket.
     * <p>
     * Crée un salon instantané en base si absent. Ouvre le salon si l'accès est autorisé.
     * </p>
     *
     * @param roomName nom de salle (trimé si valide)
     * @param userId   identifiant de l'utilisateur demandeur
     * @param userName libellé utilisateur pour journalisation
     * @return objet JSON {@code VISIO_TOKEN_RESPONSE} avec statut SUCCESS ou ERROR
     */
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

    /**
     * Planifie une réunion avec liste d'invités.
     *
     * @param roomName   nom de salle
     * @param createurId identifiant du créateur
     * @param heureProg  date-heure de début
     * @param idInvites  identifiants des invités
     * @return {@code false} si validation échoue ; sinon résultat de la persistance DAO
     */
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

    /**
     * Liste les réunions accessibles à un utilisateur (créateur ou invité).
     *
     * @param userId identifiant utilisateur ; valeur {@code <= 0} renvoie une liste vide
     * @return réunions disponibles après activation des planifiées éligibles
     */
    public List<Visio> obtenirReunionsAccessibles(int userId) {
        if (userId <= 0) {
            return new ArrayList<>();
        }
        activerSalonsPlanifiesEligibles();
        return visioDAO.listerReunionsDisponibles(userId);
    }

    /**
     * Clôture un salon : suppression si instantané, passage terminé si planifié.
     * <p>
     * Seul le créateur peut clôturer. Opération sans effet si nom invalide ou non créateur.
     * </p>
     *
     * @param roomName nom de salle
     * @param userId   identifiant de l'utilisateur demandeur (doit être créateur)
     */
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
