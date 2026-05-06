package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.PermissionDao;
import com.eseo.steevejobs.dao.PermissionDaoImpl;
import com.eseo.steevejobs.model.Enum.AppModule;
import com.eseo.steevejobs.model.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionService {

    private final PermissionDao permissionDao;

    private final Map<String, List<Integer>> cacheRoles = new HashMap<>();

    public PermissionService() {
        this.permissionDao = new PermissionDaoImpl();
    }

    public List<String> getUserPermissions(int idUser) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        return permissionDao.getPermissionCodesByUserId(idUser);
    }

    public List<Permission> getAllPermissions() {
        return permissionDao.getAllPermissions();
    }

    public List<Integer> getPermissionIdsByRole(String nomRole) {
        if (nomRole == null || nomRole.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        if (!cacheRoles.containsKey(nomRole)) {
            List<Integer> idsDepuisBDD = permissionDao.getPermissionIdsByRole(nomRole);
            cacheRoles.put(nomRole, idsDepuisBDD);
        }
        return cacheRoles.get(nomRole);
    }

    public boolean assignPermissionToRole(String nomRole, int idPermission) {
        if (nomRole == null || nomRole.trim().isEmpty() || idPermission <= 0) {
            System.out.println("Erreur métier : Paramètres invalides.");
            return false;
        }
        if (nomRole.equalsIgnoreCase("SuperAdmin")) {
            System.out.println("Erreur de sécurité : Impossible de modifier les droits du SuperAdmin depuis cette interface.");
            return false;
        }

        boolean success = permissionDao.insertRolePermission(nomRole, idPermission);

        if (success && cacheRoles.containsKey(nomRole)) {
            cacheRoles.get(nomRole).add(idPermission);
        }
        return success;
    }

    public boolean revokePermissionFromRole(String nomRole, int idPermission) {
        if (nomRole == null || nomRole.trim().isEmpty() || idPermission <= 0) {
            return false;
        }

        boolean success = permissionDao.deleteRolePermission(nomRole, idPermission);
        if (success && cacheRoles.containsKey(nomRole)) {
            cacheRoles.get(nomRole).remove(Integer.valueOf(idPermission));
        }
        return success;
    }

    public boolean createNewPermission(String codeAction, String description) {
        if (codeAction == null || codeAction.trim().isEmpty()) {
            return false;
        }
        return permissionDao.createPermission(codeAction, description);
    }

    public void synchroniserPermissionsBaseDeDonnees() {
        List<Permission> permissionsEnBase = getAllPermissions();
        List<String> codesEnBase = permissionsEnBase.stream()
                .map(Permission::getCodeAction)
                .toList();

        for (AppModule module : AppModule.values()) {
            if (!codesEnBase.contains(module.getCodeAction())) {
                String description = module.getTitle().replace("\n", " ");

                boolean success = createNewPermission(module.getCodeAction(), description);
                if (success) {
                    System.out.println("✅ Auto-génération de la permission : " + module.getCodeAction());
                }
            }
        }
    }
}