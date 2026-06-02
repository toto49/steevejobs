package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
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

    public void createUser(User user) throws SQLException {
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
        if (userDAO.emailExists(user.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe déjà");
        }
        userDAO.createUser(user);
    }

    public void updateUser(User user) throws SQLException {
        if (user.getId() <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }

        User existingUser = userDAO.getById(user.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("Aucun utilisateur trouvé avec l'ID : " + user.getId());
        }

        User userWithEmail = userDAO.getByEmail(user.getEmail());
        if (userWithEmail != null && userWithEmail.getId() != user.getId()) {
            throw new IllegalArgumentException("Cet email est déjà utilisé par un autre utilisateur");
        }

        userDAO.updateUser(user);
    }

    public boolean updateTaux(int userId, int taux) throws SQLException {
        if (taux < 0 || taux >= 100) {
            throw new IllegalArgumentException("Le taux doit être entre 0 et 99.");
        }
        return userDAO.updateTaux(userId, taux);
    }

    public boolean deleteUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deleteUser(id);
    }

    public User getUserById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.getById(id);
    }

    public User getUserByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.getByEmail(email);
    }

    /**
     * Authentifie un utilisateur en comparant le mot de passe clair avec le hash BCrypt de la BDD.
     */
    public User authenticate(String email, String passwordClair) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (passwordClair == null || passwordClair.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }

        // Récupération de l'utilisateur par son email unique
        User user = userDAO.getByEmail(email);

        if (user == null) {
            return null; // L'utilisateur n'existe pas
        }

        if (!user.isActif()) {
            throw new SecurityException("Ce compte est désactivé. Veuillez contacter l'administrateur.");
        }

        if (user.getBloqueJusqua() != null && user.getBloqueJusqua().isAfter(LocalDateTime.now())) {
            Duration duration = Duration.between(LocalDateTime.now(), user.getBloqueJusqua());
            long minutes = duration.toMinutes();
            long seconds = duration.minusMinutes(minutes).getSeconds();

            String tempsRestant = String.format("%02d min et %02d s", minutes, seconds);
            throw new SecurityException("Compte bloqué. Réessayez dans " + tempsRestant + ".");
        }

        // CORRECTION BCrypt : On vérifie le mot de passe saisi avec le hash extrait de la BDD
        boolean passwordValide = BCrypt.checkpw(passwordClair, user.getPasswordHash());

        if (!passwordValide) {
            int tentatives = user.getTentativesEchouees();
            LocalDateTime maintenant = LocalDateTime.now();
            if (user.getDateDernierEchec() != null && user.getDateDernierEchec().plusMinutes(15).isBefore(maintenant)) {
                tentatives = 0;
            }

            tentatives++;

            if (tentatives >= 5) {
                LocalDateTime finBlocage = maintenant.plusMinutes(15);
                userDAO.updateTentativesEtBlocage(user.getId(), tentatives, finBlocage, maintenant);
                String emailUser = user.getEmail();
                new Thread(() -> {
                    try {
                        MailService.EnvoyerMail(
                                emailUser,
                                "Tentative de connexion",
                                "Votre compte a subi plus de 5 tentatives de connexion. Il est temporairement bloqué."
                        );
                    } catch (Exception e) {
                        System.err.println("Impossible d'envoyer l'alerte email : " + e.getMessage());
                    }
                }).start();
                throw new SecurityException("Trop de tentatives. Compte bloqué pour 15 minutes.");
            } else {
                userDAO.updateTentativesEtBlocage(user.getId(), tentatives, null, maintenant);
                throw new SecurityException("Identifiants incorrects. Il vous reste " + (5 - tentatives) + " essai(s).");
            }
        }

        // Réinitialisation des tentatives en cas de succès
        if (user.getTentativesEchouees() > 0 || user.getDateDernierEchec() != null) {
            userDAO.updateTentativesEtBlocage(user.getId(), 0, null, null);
        }

        return user;
    }

    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    public List<User> getUsersByRole(String role) throws SQLException {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        return userDAO.findByRole(role);
    }

    public List<User> getActiveUsers() throws SQLException {
        return userDAO.findActiveUsers();
    }

    public List<User> searchUsersByName(String searchTerm) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Le terme de recherche est obligatoire");
        }
        return userDAO.searchByName(searchTerm);
    }

    public boolean deactivateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deactivateUser(id);
    }

    public boolean activateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.activateUser(id);
    }

    public boolean updateUserPassword(int id, String newPasswordClair) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        if (newPasswordClair == null || newPasswordClair.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        // CORRECTION BCrypt : Lors de la mise à jour, on applique le hachage
        String hashed = hashPassword(newPasswordClair);
        return userDAO.updatePassword(id, hashed);
    }

    public boolean checkEmailExists(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.emailExists(email);
    }

    public int getTotalUserCount() throws SQLException {
        return userDAO.countUsers();
    }

    public int getActiveUserCount() throws SQLException {
        return userDAO.countActiveUsers();
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public List<Integer> getIdsByRole(String role) throws SQLException {
        List<User> users = getUsersByRole(role);
        return users.stream()
                .map(User::getId)
                .toList();
    }
}