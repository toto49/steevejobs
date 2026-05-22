package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.PermissionDao;
import com.eseo.steevejobs.dao.PermissionDaoImpl;
import com.eseo.steevejobs.model.Enum.AppModule;
import com.eseo.steevejobs.model.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionService {

    private final PermissionDao permissionDao;

    // Cache local pour éviter des allers-retours BDD inutiles
    private final Map<String, List<Integer>> cacheRoles = new HashMap<>();

    // Rôle protégé : ses droits ne peuvent jamais être modifiés via l'interface
    private static final String ROLE_SUPER_ADMIN = "SuperAdmin";

    public PermissionService() {
        this.permissionDao = new PermissionDaoImpl();
    }

    public PermissionService(PermissionDao permissionDao) {
        this.permissionDao = permissionDao;
    }

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES
    // --------------------------------------------------------

    public List<String> obtenirPermissionsUtilisateur(int idUser)
            throws IllegalArgumentException {

        if (idUser <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        return permissionDao.getPermissionCodesByUserId(idUser);
    }

    public List<Permission> obtenirToutesLesPermissions() {
        return permissionDao.getAllPermissions();
    }

    public List<Integer> obtenirPermissionsParRole(String nomRole)
            throws IllegalArgumentException {

        validerNomRole(nomRole);

        if (!cacheRoles.containsKey(nomRole)) {
            List<Integer> ids = permissionDao.getPermissionIdsByRole(nomRole);
            cacheRoles.put(nomRole, ids);
        }
        return cacheRoles.get(nomRole);
    }

    public boolean assignerPermissionAuRole(String nomRole, int idPermission)
            throws IllegalArgumentException {

        validerNomRole(nomRole);
        validerIdPermission(idPermission);

        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(nomRole)) {
            throw new IllegalArgumentException(
                    "Impossible de modifier les droits du rôle SuperAdmin depuis cette interface.");
        }

        boolean success = permissionDao.insertRolePermission(nomRole, idPermission);
        if (success && cacheRoles.containsKey(nomRole)) {
            cacheRoles.get(nomRole).add(idPermission);
        }
        return success;
    }

    public boolean revoquerPermissionDuRole(String nomRole, int idPermission)
            throws IllegalArgumentException {

        validerNomRole(nomRole);
        validerIdPermission(idPermission);

        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(nomRole)) {
            throw new IllegalArgumentException(
                    "Impossible de modifier les droits du rôle SuperAdmin depuis cette interface.");
        }

        boolean success = permissionDao.deleteRolePermission(nomRole, idPermission);
        if (success && cacheRoles.containsKey(nomRole)) {
            cacheRoles.get(nomRole).remove(Integer.valueOf(idPermission));
        }
        return success;
    }

    public boolean creerNouvellePermission(String codeAction, String description)
            throws IllegalArgumentException {

        validerCodeAction(codeAction);

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La description de la permission est obligatoire.");
        }
        if (description.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "La description ne peut pas dépasser 255 caractères.");
        }

        return permissionDao.createPermission(codeAction.toUpperCase().trim(), description.trim());
    }

    /**
     * Synchronise automatiquement les permissions définies dans AppModule
     * avec celles présentes en base de données.
     */
    public void synchroniserPermissionsBaseDeDonnees() {
        List<Permission> permissionsEnBase = obtenirToutesLesPermissions();
        List<String> codesEnBase = permissionsEnBase.stream()
                .map(Permission::getCodeAction)
                .toList();

        for (AppModule module : AppModule.values()) {
            if (!codesEnBase.contains(module.getCodeAction())) {
                String description = module.getTitle().replace("\n", " ");
                boolean success = creerNouvellePermission(module.getCodeAction(), description);
                if (success) {
                    System.out.println("Auto-génération de la permission : " + module.getCodeAction());
                }
            }
        }
    }

    public void viderCache() {
        cacheRoles.clear();
    }

    // Méthodes conservées pour rétrocompatibilité
    public List<String> getUserPermissions(int idUser) {
        return obtenirPermissionsUtilisateur(idUser);
    }

    public List<Permission> getAllPermissions() {
        return obtenirToutesLesPermissions();
    }

    public List<Integer> getPermissionIdsByRole(String nomRole) {
        return obtenirPermissionsParRole(nomRole);
    }

    public boolean assignPermissionToRole(String nomRole, int idPermission) {
        return assignerPermissionAuRole(nomRole, idPermission);
    }

    public boolean revokePermissionFromRole(String nomRole, int idPermission) {
        return revoquerPermissionDuRole(nomRole, idPermission);
    }

    public boolean createNewPermission(String codeAction, String description) {
        if (codeAction == null || codeAction.trim().isEmpty()) return false;
        String desc = (description != null) ? description : "";
        return permissionDao.createPermission(codeAction.toUpperCase().trim(), desc);
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerNomRole(String nomRole) throws IllegalArgumentException {
        if (nomRole == null || nomRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du rôle est obligatoire.");
        }
        if (nomRole.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Le nom du rôle ne peut pas dépasser 50 caractères.");
        }
    }

    private void validerIdPermission(int idPermission) throws IllegalArgumentException {
        if (idPermission <= 0) {
            throw new IllegalArgumentException("L'ID de la permission est invalide.");
        }
    }

    private void validerCodeAction(String codeAction) throws IllegalArgumentException {
        if (codeAction == null || codeAction.trim().isEmpty()) {
            throw new IllegalArgumentException("Le code action est obligatoire.");
        }
        if (codeAction.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Le code action ne peut pas dépasser 100 caractères.");
        }
        // Un code action valide ne contient que des lettres majuscules, chiffres et underscore
        if (!codeAction.trim().matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Le code action ne doit contenir que des lettres majuscules, chiffres et underscores (ex : APP_MODULE_VIEW).");
        }
    }
}
