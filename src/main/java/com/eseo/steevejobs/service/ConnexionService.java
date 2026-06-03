package com.eseo.steevejobs.service;

import java.security.SecureRandom;

/**
 * Génération de secrets pour l'authentification (mots de passe provisoires, réinitialisation).
 * <p>
 * Aucun effet de bord réseau : sortie alphanumérique locale via {@link SecureRandom}.
 * </p>
 */
public class ConnexionService {

    /**
     * Génère une chaîne aléatoire pour usage comme mot de passe temporaire.
     *
     * @param length longueur souhaitée de la chaîne (doit être strictement positive côté appelant)
     * @return mot de passe composé de lettres majuscules, minuscules et chiffres
     */
    public static String generateRandomMdp(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}
