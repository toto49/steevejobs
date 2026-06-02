package com.eseo.steevejobs.config;

import com.eseo.steevejobs.model.Permission;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.PermissionService;
import com.eseo.steevejobs.service.UserService;

import java.util.List;

public class DatabaseSeeder {

    public static void main(String[] args) {
        UserService userService = new UserService();
        PermissionService permissionService = new PermissionService();

        try {
            if (!userService.checkEmailExists("admin@admin.fr")) {
                User admin = new User();
                admin.setNom("Administrateur");
                admin.setPrenom("Principal");
                admin.setEmail("admin@admin.fr");
                String hashedMdp = userService.hashPassword("admin123");
                admin.setPasswordHash(hashedMdp);
                admin.setRole("ADMIN");
                admin.setPoste("Directeur Général");
                admin.setActif(true);
                admin.setTaux(1);
                userService.createUser(admin);
                System.out.println("✅ Compte administrateur créé avec succès avec jBCrypt !");
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
}