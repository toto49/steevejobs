package com.eseo.steevejobs.service;

import java.util.prefs.Preferences;

public class SessionService {
    private static final String CLE_EMAIL = "email_utilisateur";
    private final Preferences prefs;

    public SessionService() {
        this.prefs = Preferences.userNodeForPackage(SessionService.class);
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
