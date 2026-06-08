package com.eseo.steevejobs.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Date;

/**
 * Émission de jetons JWT pour l'enregistrement des clients WebSocket.
 * <p>
 * Le secret est lu depuis la variable d'environnement {@code JWT_SECRET} (fichier .env).
 * Durée de validité : 24 heures. Émetteur fixe : {@code steevejobs-api}.
 * Aucun effet de bord persistant en cas d'échec (journalisation console, retour {@code null}).
 * </p>
 */
public class JwtService {

    /**
     * Construit un JWT HMAC256 portant l'identifiant utilisateur en sujet.
     *
     * @param userId identifiant technique de l'utilisateur
     * @return jeton signé, ou {@code null} si {@code JWT_SECRET} est absent ou en cas d'erreur de signature
     */
    public static String genererToken(int userId) {
        try {
            String secret = Dotenv.load().get("JWT_SECRET");

            if (secret == null || secret.isEmpty()) {
                System.err.println("ERREUR : JWT_SECRET introuvable dans le .env client");
                return null;
            }

            Algorithm algorithm = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("steevejobs-api")
                    .withSubject(String.valueOf(userId))
                    .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))
                    .sign(algorithm);

        } catch (Exception e) {
            System.err.println("Erreur JWT : " + e.getMessage());
            return null;
        }
    }
}
