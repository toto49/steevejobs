package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.PermissionDAO;
import com.eseo.steevejobs.dao.PermissionDAOImpl;
import com.eseo.steevejobs.model.Enum.AppModule;
import com.eseo.steevejobs.model.Permission;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des permissions applicatives et de leur attribution par rôle.
 * <p>
 * Règles métier : le rôle {@code SuperAdmin} ne peut pas être modifié via cette couche ;
 * cache mémoire des identifiants de permission par rôle ; synchronisation automatique
 * des codes {@link AppModule} manquants en base. Aucun effet réseau.
 * </p>
 */
public class PermissionService {

    private final PermissionDAO permissionDAO;
    private final Map<String, List<Integer>> roleCache = new HashMap<>();
    private static final String ROLE_SUPER_ADMIN = "SuperAdmin";

    /**
     * Constructeur par défaut avec {@link PermissionDAOImpl}.
     */
    public PermissionService() {
        this.permissionDAO = new PermissionDAOImpl();
    }

    /**
     * Constructeur avec injection du DAO.
     *
     * @param permissionDAO accès persistance permissions
     */
    public PermissionService(PermissionDAO permissionDAO) {
        this.permissionDAO = permissionDAO;
    }

    /**
     * Retourne les codes de permission effectifs d'un utilisateur.
     *
     * @param idUser identifiant utilisateur strictement positif
     * @return liste des codes action
     * @throws IllegalArgumentException si l'identifiant est invalide
     */
    public List<String> getUserPermissions(int idUser) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("L'ID utilisateur est invalide.");
        }
        return permissionDAO.getPermissionCodesByUserId(idUser);
    }

    /**
     * Liste toutes les permissions référencées en base.
     *
     * @return catalogue des permissions
     */
    public List<Permission> getAllPermissions() {
        return permissionDAO.getAllPermissions();
    }

    /**
     * Retourne les identifiants de permission liés à un rôle (avec cache).
     *
     * @param nomRole nom du rôle
     * @return liste d'identifiants de permission
     * @throws IllegalArgumentException si le nom de rôle est invalide
     */
    public List<Integer> getPermissionIdsByRole(String nomRole) {
        validerNomRole(nomRole);

        if (!roleCache.containsKey(nomRole)) {
            List<Integer> ids = permissionDAO.getPermissionIdsByRole(nomRole);
            roleCache.put(nomRole, ids);
        }
        return roleCache.get(nomRole);
    }

    /**
     * Associe une permission à un rôle et met à jour le cache si présent.
     *
     * @param nomRole       nom du rôle (hors SuperAdmin)
     * @param idPermission  identifiant de permission
     * @return {@code true} si l'insertion a réussi
     * @throws IllegalArgumentException si rôle SuperAdmin ou paramètres invalides
     */
    public boolean assignPermissionToRole(String nomRole, int idPermission) {
        validerNomRole(nomRole);
        validerIdPermission(idPermission);

        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(nomRole)) {
            throw new IllegalArgumentException(
                    "Impossible de modifier les droits du rôle SuperAdmin depuis cette interface.");
        }

        boolean success = permissionDAO.insertRolePermission(nomRole, idPermission);
        if (success && roleCache.containsKey(nomRole)) {
            roleCache.get(nomRole).add(idPermission);
        }
        return success;
    }

    /**
     * Retire une permission d'un rôle et met à jour le cache si présent.
     *
     * @param nomRole       nom du rôle (hors SuperAdmin)
     * @param idPermission  identifiant de permission
     * @return {@code true} si la suppression de liaison a réussi
     * @throws IllegalArgumentException si rôle SuperAdmin ou paramètres invalides
     */
    public boolean revokePermissionFromRole(String nomRole, int idPermission) {
        validerNomRole(nomRole);
        validerIdPermission(idPermission);

        if (ROLE_SUPER_ADMIN.equalsIgnoreCase(nomRole)) {
            throw new IllegalArgumentException(
                    "Impossible de modifier les droits du rôle SuperAdmin depuis cette interface.");
        }

        boolean success = permissionDAO.deleteRolePermission(nomRole, idPermission);
        if (success && roleCache.containsKey(nomRole)) {
            roleCache.get(nomRole).remove(Integer.valueOf(idPermission));
        }
        return success;
    }

    /**
     * Crée une nouvelle permission applicative.
     *
     * @param codeAction  code normalisé (majuscules, chiffres, underscore)
     * @param description libellé descriptif (max 255 caractères)
     * @return {@code true} si la création a réussi
     * @throws IllegalArgumentException si code ou description invalides
     */
    public boolean createNewPermission(String codeAction, String description) {
        validerCodeAction(codeAction);

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La description de la permission est obligatoire.");
        }
        if (description.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "La description ne peut pas dépasser 255 caractères.");
        }

        return permissionDAO.createPermission(codeAction.toUpperCase().trim(), description.trim());
    }

    /**
     * Crée en base les permissions manquantes pour chaque {@link AppModule} déclaré.
     * <p>
     * Effet de bord console : journalisation des créations automatiques réussies.
     * </p>
     */
    public void syncAppModulePermissions() {
        List<Permission> permissionsEnBase = getAllPermissions();
        List<String> codesEnBase = permissionsEnBase.stream()
                .map(Permission::getCodeAction)
                .toList();

        for (AppModule module : AppModule.values()) {
            if (!codesEnBase.contains(module.getCodeAction())) {
                String description = module.getTitle().replace("\n", " ");
                boolean success = createNewPermission(module.getCodeAction(), description);
                if (success) {
                    System.out.println("Auto-génération de la permission : " + module.getCodeAction());
                }
            }
        }
    }

    private void validerNomRole(String nomRole) {
        if (nomRole == null || nomRole.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du rôle est obligatoire.");
        }
        if (nomRole.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Le nom du rôle ne peut pas dépasser 50 caractères.");
        }
    }

    private void validerIdPermission(int idPermission) {
        if (idPermission <= 0) {
            throw new IllegalArgumentException("L'ID de la permission est invalide.");
        }
    }

    private void validerCodeAction(String codeAction) {
        if (codeAction == null || codeAction.trim().isEmpty()) {
            throw new IllegalArgumentException("Le code action est obligatoire.");
        }
        if (codeAction.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Le code action ne peut pas dépasser 100 caractères.");
        }
        if (!codeAction.trim().matches("[A-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Le code action ne doit contenir que des lettres majuscules, chiffres et underscores (ex : APP_MODULE_VIEW).");
        }
    }
}
