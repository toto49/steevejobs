package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service métier dédié aux opérations sur les utilisateurs.
 * <p>
 * Fait le lien entre la couche DAO et la couche de présentation (controllers/views).
 * Contient la logique métier et la validation des données.
 * </p>
 */
public class UserService {

    private final UserDAO userDAO;

    /**
     * Constructeur par défaut qui initialise le DAO
     */
    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Constructeur avec injection de dépendance.
     *
     * @param userDAO le DAO à utiliser
     */
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Crée un nouvel utilisateur.
     *
     * @param user l'utilisateur à créer
     * @throws SQLException si une erreur SQL survient
     * @throws IllegalArgumentException si l'email existe déjà
     */
    public void createUser(User user) throws SQLException {
        // Validation des données
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (user.getNom() == null || user.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }

        // Vérifier si l'email existe déjà
        if (userDAO.emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }

        userDAO.createUser(user);
    }

    /**
     * Met à jour un utilisateur existant.
     *
     * @param user l'utilisateur à mettre à jour
     * @throws SQLException si une erreur SQL survient
     * @throws IllegalArgumentException si l'utilisateur n'existe pas
     */
    public void updateUser(User user) throws SQLException {
        // Validation
        if (user.getId() <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        // Vérifier si l'utilisateur existe
        User existingUser = userDAO.getById(user.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouvé avec l'ID : " + user.getId());
        }

        // Vérifier si le nouvel email n'est pas déjà utilisé par un autre utilisateur
        User userWithEmail = userDAO.getByEmail(user.getEmail());
        if (userWithEmail != null && userWithEmail.getId() != user.getId()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur");
        }

        userDAO.updateUser(user);
    }

    /**
     * Supprime un utilisateur.
     *
     * @param id l'ID de l'utilisateur
     * @return true si supprimé, false sinon
     * @throws SQLException si une erreur SQL survient
     */
    public boolean deleteUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deleteUser(id);
    }

    /**
     * Récupère un utilisateur par son ID.
     *
     * @param id l'ID de l'utilisateur
     * @return l'utilisateur trouvé, null sinon
     * @throws SQLException si une erreur SQL survient
     */
    public User getUserById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.getById(id);
    }

    /**
     * Récupère un utilisateur par son email.
     *
     * @param email l'email de l'utilisateur
     * @return l'utilisateur trouvé, null sinon
     * @throws SQLException si une erreur SQL survient
     */
    public User getUserByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.getByEmail(email);
    }

    /**
     * Authentifie un utilisateur.
     *
     * @param email        l'email
     * @param passwordHash le hash du mot de passe
     * @return l'utilisateur authentifié, null sinon
     * @throws SQLException si une erreur SQL survient
     */
    public User authenticate(String email, String passwordHash) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        User user = userDAO.authenticate(email, passwordHash);

        if (user != null) {
            if (!user.isActif()) {
                throw new IllegalStateException("Ce compte est désactivé. Veuillez contacter l'administrateur.");
            }
        }

        return user;
    }

    /**
     * Récupère tous les utilisateurs.
     *
     * @return la liste de tous les utilisateurs
     * @throws SQLException si une erreur SQL survient
     */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    /**
     * Récupère les utilisateurs par rôle.
     *
     * @param role le rôle
     * @return la liste des utilisateurs ayant ce rôle
     * @throws SQLException si une erreur SQL survient
     */
    public List<User> getUsersByRole(String role) throws SQLException {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        return userDAO.findByRole(role);
    }

    /**
     * Récupère les utilisateurs actifs.
     *
     * @return la liste des utilisateurs actifs
     * @throws SQLException si une erreur SQL survient
     */
    public List<User> getActiveUsers() throws SQLException {
        return userDAO.findActiveUsers();
    }

    /**
     * Recherche des utilisateurs par nom ou prénom.
     *
     * @param searchTerm le terme de recherche
     * @return la liste des utilisateurs correspondants
     * @throws SQLException si une erreur SQL survient
     */
    public List<User> searchUsersByName(String searchTerm) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Le terme de recherche est obligatoire");
        }
        return userDAO.searchByName(searchTerm);
    }

    /**
     * Désactive un utilisateur.
     *
     * @param id l'ID de l'utilisateur
     * @return true si désactivé, false sinon
     * @throws SQLException si une erreur SQL survient
     */
    public boolean deactivateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deactivateUser(id);
    }

    /**
     * Active un utilisateur.
     *
     * @param id l'ID de l'utilisateur
     * @return true si activé, false sinon
     * @throws SQLException si une erreur SQL survient
     */
    public boolean activateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.activateUser(id);
    }

    /**
     * Met à jour le mot de passe d'un utilisateur.
     *
     * @param id              l'ID de l'utilisateur
     * @param newPasswordHash le nouveau hash du mot de passe
     * @return true si mis à jour, false sinon
     * @throws SQLException si une erreur SQL survient
     */
    public boolean updateUserPassword(int id, String newPasswordHash) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        return userDAO.updatePassword(id, newPasswordHash);
    }

    /**
     * Vérifie si un email existe déjà.
     *
     * @param email l'email à vérifier
     * @return true si l'email existe, false sinon
     * @throws SQLException si une erreur SQL survient
     */
    public boolean checkEmailExists(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.emailExists(email);
    }

    /**
     * Compte le nombre total d'utilisateurs.
     *
     * @return le nombre total d'utilisateurs
     * @throws SQLException si une erreur SQL survient
     */
    public int getTotalUserCount() throws SQLException {
        return userDAO.countUsers();
    }

    /**
     * Compte le nombre d'utilisateurs actifs.
     *
     * @return le nombre d'utilisateurs actifs
     * @throws SQLException si une erreur SQL survient
     */
    public int getActiveUserCount() throws SQLException {
        return userDAO.countActiveUsers();
    }

    public String hashPassword(String password) {
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

    public List<Integer> getIdsByRole(String role) throws SQLException {
        List<User> users = getUsersByRole(role);
        return users.stream()
                .map(User::getId)
                .toList();
    }
}