package com.eseo.steevejobs.service;

import java.security.SecureRandom;

public class ConnexionService {

    public static String generateRandomMdp(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        //todo: appeler le DAO de changement de mot de passe
        return sb.toString();
    }
}
