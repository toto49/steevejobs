package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.VisioDAO;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.Visio;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VisioService {

    private final VisioDAO visioDAO = new VisioDAO();

    public JSONObject traiterDemandeConnexion(String roomName, int userId, String userName) {
        JSONObject reponse = new JSONObject();

        if (roomName == null || roomName.trim().isEmpty()) {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "⚠️ Le nom de la salle est invalide.");
            return reponse;
        }

        roomName = roomName.trim();

        if (!visioDAO.existeEnBdd(roomName)) {
            System.out.println("✨ Initialisation d'une visioconférence instantanée publique : " + roomName);
            Visio instantAppel = new Visio(roomName, userId, null);
            instantAppel.setStatut(VisioStatut.EN_COURS);
            visioDAO.enregistrerSalonInstantane(instantAppel);
        }

        visioDAO.ouvrirSalon(roomName);

        int codeAcces = visioDAO.verifierAccesSalon(roomName, userId);

        if (codeAcces == 1) {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "SUCCESS");
            reponse.put("roomName", roomName);
            System.out.println("✅ Autorisation d'accès BDD accordée pour " + userName + " sur le salon [" + roomName + "]");

        } else if (codeAcces == -1) {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "⚠️ Cette réunion est verrouillée. Elle n'ouvrira que quelques minutes avant l'heure prévue.");
        } else {
            reponse.put("type", "VISIO_TOKEN_RESPONSE");
            reponse.put("status", "ERROR");
            reponse.put("message", "❌ Accès refusé : Vous ne figurez pas sur le registre des invités de cette session.");
        }

        return reponse;
    }

    public boolean planifierNouvelleReunion(String roomName, int createurId, LocalDateTime heureProg, List<Integer> idInvites) {
        if (roomName == null || roomName.trim().isEmpty() || heureProg == null) {
            return false;
        }
        Visio planification = new Visio(roomName.trim(), createurId, heureProg);
        planification.setStatut(VisioStatut.PROGRAMMEE);
        return visioDAO.planifierReunion(planification, idInvites);
    }

    public List<Visio> obtenirReunionsAccessibles(int userId) {
        if (userId <= 0) return new ArrayList<>();
        return visioDAO.listerReunionsDisponibles(userId);
    }

    public void gererDepartParticipant(String roomName, int userId) {
        System.out.println("🚶 Profil #" + userId + " déconnecté du salon : " + roomName);
    }

    public void couperSalonDefinitif(String roomName, int userId) {
        if (visioDAO.isCreateur(roomName, userId)) {
            visioDAO.terminerSalon(roomName);
            System.out.println("🔒 Le créateur a révoqué l'accès au salon : " + roomName);
        }
    }
}