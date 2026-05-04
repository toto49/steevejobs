package com.eseo.steevejobs.util;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.UserService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseSeeder {

    public static void main(String[] args) {
        UserService userService = new UserService();

        try {
            if (!userService.checkEmailExists("admin@admin.fr")) {
                User admin = new User();
                admin.setNom("Administrateur");
                admin.setEmail("admin@admin.fr");
                admin.setPasswordHash(hashPassword("admin123"));
                admin.setRole("ADMIN");
                admin.setPoste("Directeur Général");
                userService.createUser(admin);
                System.out.println("Compte administrateur créé avec succès !");
            } else {
                System.out.println("L'administrateur existe déjà.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors du hachage du mot de passe", e);
        }
    }
}