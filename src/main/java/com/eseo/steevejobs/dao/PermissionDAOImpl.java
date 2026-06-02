package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Permission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PermissionDAOImpl implements PermissionDAO {

    @Override
    public List<String> getPermissionCodesByUserId(int idUser) {
        List<String> permissions = new ArrayList<>();
        String sql = "SELECT p.code_action FROM USER u " +
                "JOIN ROLE_PERMISSION rp ON u.role = rp.nom_role " +
                "JOIN PERMISSION p ON rp.id_permission = p.id_permission " +
                "WHERE u.id_user = ? AND u.actif = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUser);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                permissions.add(rs.getString("code_action"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur DAO (getPermissions) : " + e.getMessage());
        }
        return permissions;
    }

    @Override
    public boolean insertRolePermission(String nomRole, int idPermission) {
        String sql = "INSERT INTO ROLE_PERMISSION (nom_role, id_permission) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomRole);
            stmt.setInt(2, idPermission);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur DAO (insertRolePermission) : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteRolePermission(String nomRole, int idPermission) {
        String sql = "DELETE FROM ROLE_PERMISSION WHERE nom_role = ? AND id_permission = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomRole);
            stmt.setInt(2, idPermission);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur DAO (deleteRolePermission) : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean createPermission(String codeAction, String description) {
        String sql = "INSERT INTO PERMISSION (code_action, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codeAction.toUpperCase());
            stmt.setString(2, description);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur DAO (createPermission) : " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Permission> getAllPermissions() {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT * FROM PERMISSION";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Permission(
                        rs.getInt("id_permission"),
                        rs.getString("code_action"),
                        rs.getString("description")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Integer> getPermissionIdsByRole(String nomRole) {
        List<Integer> list = new ArrayList<>();
        String sql = "SELECT id_permission FROM ROLE_PERMISSION WHERE nom_role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomRole);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getInt("id_permission"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
