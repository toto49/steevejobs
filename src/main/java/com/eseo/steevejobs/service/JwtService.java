package com.eseo.steevejobs.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Date;

public class JwtService {

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