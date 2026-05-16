package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.User;
import java.util.prefs.Preferences;

public class SessionService {
    private static User utilisateurConnecte;

    private static String tokenJWT;
    private static final String CLE_EMAIL = "email_utilisateur";
    private final Preferences prefs;

    public SessionService() {
        this.prefs = Preferences.userNodeForPackage(SessionService.class);
    }

    public static User getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    public static void setUtilisateurConnecte(User user) {
        utilisateurConnecte = user;
    }

    public static String getTokenJWT() {
        return tokenJWT;
    }

    public static void setTokenJWT(String token) {
        tokenJWT = token;
    }


    public void sauvegarderEmail(String email) {
        prefs.put(CLE_EMAIL, email);
    }

    public String recupererEmail() {
        return prefs.get(CLE_EMAIL, "");
    }

    public void effacerEmail() {
        prefs.remove(CLE_EMAIL);
    }

    public boolean hasEmailSauvegarde() {
        return !recupererEmail().isEmpty();
    }
}