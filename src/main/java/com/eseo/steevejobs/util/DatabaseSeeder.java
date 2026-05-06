package com.eseo.steevejobs.util;

import com.eseo.steevejobs.model.Permission;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PermissionService;
import com.eseo.steevejobs.service.UserService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class DatabaseSeeder {

    static void main(String[] args) {
        UserService userService = new UserService();
        PermissionService permissionService = new PermissionService();

        try {
            if (!userService.checkEmailExists("admin@admin.fr")) {
                User admin = new User();
                admin.setNom("Administrateur");
                admin.setEmail("admin@admin.fr");
                admin.setPasswordHash(hashPassword("admin123"));
                admin.setRole("ADMIN");
                admin.setPoste("Directeur Général");
                admin.setActif(true);
                userService.createUser(admin);
                System.out.println("✅ Compte administrateur créé avec succès !");
            } else {
                System.out.println("ℹ️ L'administrateur existe déjà.");
            }
            String codeAdmin = "APP_ADMINPANEL_VIEW";
            permissionService.createNewPermission(codeAdmin, "Accès total au panneau d'administration");
            List<Permission> toutesLesPerms = permissionService.getAllPermissions();
            int idPermissionAdmin = -1;

            for (Permission p : toutesLesPerms) {
                if (p.getCodeAction().equalsIgnoreCase(codeAdmin)) {
                    idPermissionAdmin = p.getId();
                    break;
                }
            }
            if (idPermissionAdmin != -1) {
                List<Integer> permsDuRoleAdmin = permissionService.getPermissionIdsByRole("ADMIN");

                if (!permsDuRoleAdmin.contains(idPermissionAdmin)) {
                    permissionService.assignPermissionToRole("ADMIN", idPermissionAdmin);
                    System.out.println("✅ Permission '" + codeAdmin + "' assignée au rôle ADMIN !");
                } else {
                    System.out.println("ℹ️ Le rôle ADMIN possède déjà la permission d'administration.");
                }
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