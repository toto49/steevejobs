package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Permission;

import java.util.List;

/**
 * Contrat d'accès aux données des tables {@code PERMISSION} et {@code ROLE_PERMISSION}.
 * <p>
 * Les implémentations interrogent la base via {@link DatabaseConnection}.
 * La stratégie de gestion d'erreur (propagation ou journalisation) dépend de l'implémentation.
 * </p>
 *
 * @see PermissionDAOImpl
 */
public interface PermissionDAO {

    /**
     * Récupère les codes d'action autorisés pour un utilisateur actif.
     * <p>
     * SQL : jointure {@code USER} → {@code ROLE_PERMISSION} → {@code PERMISSION},
     * filtrée par {@code id_user} et {@code actif = TRUE}.
     * </p>
     *
     * @param idUser identifiant de l'utilisateur
     * @return liste des codes d'action (vide si aucune permission ou en cas d'erreur gérée)
     */
    List<String> getPermissionCodesByUserId(int idUser);

    /**
     * Associe une permission à un rôle.
     * <p>
     * SQL : {@code INSERT INTO ROLE_PERMISSION}.
     * </p>
     *
     * @param nomRole       nom du rôle
     * @param idPermission  identifiant de la permission
     * @return {@code true} si l'association a été créée
     */
    boolean insertRolePermission(String nomRole, int idPermission);

    /**
     * Supprime l'association entre un rôle et une permission.
     * <p>
     * SQL : {@code DELETE FROM ROLE_PERMISSION WHERE nom_role = ? AND id_permission = ?}.
     * </p>
     *
     * @param nomRole       nom du rôle
     * @param idPermission  identifiant de la permission
     * @return {@code true} si au moins une ligne a été supprimée
     */
    boolean deleteRolePermission(String nomRole, int idPermission);

    /**
     * Crée une nouvelle permission applicative.
     * <p>
     * SQL : {@code INSERT INTO PERMISSION} ; le code d'action est converti en majuscules.
     * </p>
     *
     * @param codeAction  code unique de l'action
     * @param description libellé descriptif
     * @return {@code true} si la permission a été insérée
     */
    boolean createPermission(String codeAction, String description);

    /**
     * Liste l'ensemble des permissions enregistrées.
     * <p>
     * SQL : {@code SELECT * FROM PERMISSION}.
     * </p>
     *
     * @return liste des permissions (vide si aucune ou en cas d'erreur gérée)
     */
    List<Permission> getAllPermissions();

    /**
     * Récupère les identifiants de permission associés à un rôle.
     * <p>
     * SQL : {@code SELECT id_permission FROM ROLE_PERMISSION WHERE nom_role = ?}.
     * </p>
     *
     * @param nomRole nom du rôle
     * @return liste des identifiants de permission (vide si aucune ou en cas d'erreur gérée)
     */
    List<Integer> getPermissionIdsByRole(String nomRole);
}
