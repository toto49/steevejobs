package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Permission;

import java.util.List;

public interface PermissionDao {
    List<String> getPermissionCodesByUserId(int idUser);

    boolean insertRolePermission(String nomRole, int idPermission);

    boolean deleteRolePermission(String nomRole, int idPermission);

    boolean createPermission(String codeAction, String description);

    List<Permission> getAllPermissions();

    List<Integer> getPermissionIdsByRole(String nomRole);
}