package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.User;
import java.util.prefs.Preferences;

/**
 * Gestion de la session applicative côté client JavaFX (état en mémoire et préférences locales).
 * <p>
 * Stocke l'utilisateur connecté, le jeton JWT pour le WebSocket, et optionnellement
 * l'e-mail de connexion dans {@link Preferences} (aucun effet réseau).
 * </p>
 */
public class SessionService {
    /** Utilisateur actuellement authentifié en session applicative. */
    private static User utilisateurConnecte;

    /** Jeton JWT émis pour l'enregistrement WebSocket. */
    private static String tokenJWT;
    /** Clé de préférence locale pour l'e-mail de connexion mémorisé. */
    private static final String CLE_EMAIL = "email_utilisateur";
    /** Nœud de préférences utilisateur pour la persistance locale de session. */
    private final Preferences prefs;

    /**
     * Initialise l'accès aux préférences utilisateur du package.
     */
    public SessionService() {
        this.prefs = Preferences.userNodeForPackage(SessionService.class);
    }

    /**
     * Retourne l'utilisateur actuellement authentifié en session.
     *
     * @return utilisateur connecté, ou {@code null} si déconnecté
     */
    public static User getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    /**
     * Associe l'utilisateur à la session applicative.
     *
     * @param user utilisateur connecté
     */
    public static void setUtilisateurConnecte(User user) {
        utilisateurConnecte = user;
    }

    /**
     * Retourne le jeton JWT utilisé pour l'enregistrement WebSocket.
     *
     * @return jeton JWT, ou {@code null} si non défini
     */
    public static String getTokenJWT() {
        return tokenJWT;
    }

    /**
     * Enregistre le jeton JWT de la session.
     *
     * @param token jeton JWT
     */
    public static void setTokenJWT(String token) {
        tokenJWT = token;
    }

    /**
     * Mémorise l'adresse e-mail de connexion dans les préférences locales.
     *
     * @param email adresse e-mail à conserver
     */
    public void sauvegarderEmail(String email) {
        prefs.put(CLE_EMAIL, email);
    }

    /**
     * Lit l'adresse e-mail mémorisée.
     *
     * @return e-mail sauvegardé, ou chaîne vide si absent
     */
    public String recupererEmail() {
        return prefs.get(CLE_EMAIL, "");
    }

    /**
     * Supprime l'e-mail mémorisé des préférences.
     */
    public void effacerEmail() {
        prefs.remove(CLE_EMAIL);
    }

    /**
     * Indique si un e-mail a été mémorisé.
     *
     * @return {@code true} si une valeur non vide est stockée
     */
    public boolean hasEmailSauvegarde() {
        return !recupererEmail().isEmpty();
    }
}
