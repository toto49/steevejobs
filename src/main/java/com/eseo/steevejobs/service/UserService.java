package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Opérations métier sur les comptes utilisateurs et l'authentification.
 * <p>
 * Règles métier : email unique ; compte actif requis à la connexion ;
 * blocage temporaire après 5 échecs (15 minutes, fenêtre de réinitialisation des tentatives).
 * Effet de bord à l'authentification : envoi asynchrone d'un e-mail d'alerte via
 * {@link MailService} en cas de blocage. Hachage des mots de passe avec BCrypt (coût 12).
 * </p>
 */
public class UserService {

    /** Accès persistance aux comptes utilisateurs. */
    private final UserDAO userDAO;

    /**
     * Constructeur par défaut instanciant un {@link UserDAO}.
     */
    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Constructeur avec injection du DAO.
     *
     * @param userDAO accès persistance utilisateurs
     */
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Crée un utilisateur après validation, contrôle d'unicité de l'email,
     * et hachage automatique du mot de passe en jBCrypt.
     *
     * @param user entité utilisateur contenant le mot de passe en clair
     * @throws IllegalArgumentException si champs obligatoires manquants ou email existant
     * @throws SQLException             en cas d'erreur d'accès base
     */
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
        String mdp = user.getPasswordHash();
        if (!isBcryptHash(mdp)) {
            mdp = hashPassword(mdp);
        }
        user.setPasswordHash(mdp);

        userDAO.createUser(user);
    }

    /**
     * Met à jour le profil d'un utilisateur existant (données personnelles, rôle, etc.).
     * Le mot de passe n'est jamais modifié ici : utiliser {@link #updateUserPassword(int, String)}.
     *
     * @param user utilisateur avec identifiant valide et champs à persister
     * @throws IllegalArgumentException si identifiant ou email invalides
     * @throws SQLException             en cas d'erreur d'accès base
     */
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

        existingUser.setNom(user.getNom());
        existingUser.setPrenom(user.getPrenom());
        existingUser.setEmail(user.getEmail());
        existingUser.setAdresse(user.getAdresse());
        existingUser.setTel(user.getTel());
        existingUser.setRole(user.getRole());
        existingUser.setPoste(user.getPoste());
        existingUser.setActif(user.isActif());
        existingUser.setTaux(user.getTaux());

        userDAO.updateUser(existingUser);
    }

    /**
     * Met à jour le taux horaire ou assimilé d'un utilisateur (0–99).
     *
     * @param userId identifiant utilisateur
     * @param taux   valeur entière strictement inférieure à 100
     * @return {@code true} si la mise à jour a réussi
     * @throws IllegalArgumentException si le taux est hors plage
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean updateTaux(int userId, int taux) throws SQLException {
        if (taux < 0 || taux >= 100) {
            throw new IllegalArgumentException("Le taux doit être entre 0 et 99.");
        }
        return userDAO.updateTaux(userId, taux);
    }

    /**
     * Supprime un utilisateur.
     *
     * @param id identifiant utilisateur
     * @return {@code true} si la suppression a réussi
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean deleteUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deleteUser(id);
    }

    /**
     * Charge un utilisateur par identifiant.
     *
     * @param id identifiant utilisateur
     * @return utilisateur ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public User getUserById(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.getById(id);
    }

    /**
     * Charge un utilisateur par adresse e-mail.
     *
     * @param email adresse e-mail
     * @return utilisateur ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'email est vide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public User getUserByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.getByEmail(email);
    }

    /**
     * Authentifie un utilisateur par e-mail et mot de passe clair (BCrypt).
     *
     * @param email         adresse e-mail
     * @param passwordClair mot de passe en clair
     * @return utilisateur authentifié, ou {@code null} si compte inexistant
     * @throws IllegalArgumentException si email ou mot de passe vide
     * @throws SecurityException        si compte désactivé, bloqué ou mot de passe incorrect
     * @throws Exception                propagation d'erreurs DAO éventuelles
     */
    public User authenticate(String email, String passwordClair) throws Exception {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (passwordClair == null || passwordClair.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }

        User user = userDAO.getByEmail(email);

        if (user == null) {
            return null;
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

        if (user.getTentativesEchouees() > 0 || user.getDateDernierEchec() != null) {
            userDAO.updateTentativesEtBlocage(user.getId(), 0, null, null);
        }

        return user;
    }

    /**
     * Liste tous les utilisateurs.
     *
     * @return liste complète
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    /**
     * Liste les utilisateurs d'un rôle donné.
     *
     * @param role nom du rôle
     * @return utilisateurs du rôle
     * @throws IllegalArgumentException si le rôle est vide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<User> getUsersByRole(String role) throws SQLException {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        return userDAO.findByRole(role);
    }

    /**
     * Liste les comptes actifs.
     *
     * @return utilisateurs actifs
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<User> getActiveUsers() throws SQLException {
        return userDAO.findActiveUsers();
    }

    /**
     * Recherche des utilisateurs par fragment de nom.
     *
     * @param searchTerm terme de recherche non vide
     * @return résultats correspondants
     * @throws IllegalArgumentException si le terme est vide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<User> searchUsersByName(String searchTerm) throws SQLException {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Le terme de recherche est obligatoire");
        }
        return userDAO.searchByName(searchTerm);
    }

    /**
     * Désactive un compte utilisateur.
     *
     * @param id identifiant utilisateur
     * @return {@code true} si la désactivation a réussi
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean deactivateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.deactivateUser(id);
    }

    /**
     * Réactive un compte utilisateur.
     *
     * @param id identifiant utilisateur
     * @return {@code true} si l'activation a réussi
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean activateUser(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        return userDAO.activateUser(id);
    }

    /**
     * Remplace le mot de passe par un nouveau hash BCrypt.
     *
     * @param id                identifiant utilisateur
     * @param newPasswordClair  nouveau mot de passe en clair
     * @return {@code true} si la mise à jour a réussi
     * @throws IllegalArgumentException si identifiant ou mot de passe invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean updateUserPassword(int id, String newPasswordClair) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID utilisateur invalide");
        }
        if (newPasswordClair == null || newPasswordClair.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        String hashed = hashPassword(newPasswordClair);
        return userDAO.updatePassword(id, hashed);
    }

    /**
     * Indique si un e-mail est déjà enregistré.
     *
     * @param email adresse e-mail
     * @return {@code true} si l'e-mail existe
     * @throws IllegalArgumentException si l'email est vide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean checkEmailExists(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        return userDAO.emailExists(email);
    }

    /**
     * Retourne le nombre total d'utilisateurs.
     *
     * @return effectif total
     * @throws SQLException en cas d'erreur d'accès base
     */
    public int getTotalUserCount() throws SQLException {
        return userDAO.countUsers();
    }

    /**
     * Retourne le nombre d'utilisateurs actifs.
     *
     * @return effectif actif
     * @throws SQLException en cas d'erreur d'accès base
     */
    public int getActiveUserCount() throws SQLException {
        return userDAO.countActiveUsers();
    }

    /**
     * Met à jour le taux de cotisations patronales (0–99).
     *
     * @param userId       identifiant utilisateur
     * @param tauxPatronal taux entier
     * @return {@code true} si la mise à jour a réussi
     * @throws IllegalArgumentException si le taux est hors plage
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public boolean updateTauxPatronal(int userId, int tauxPatronal) throws SQLException {
        if (tauxPatronal < 0 || tauxPatronal >= 100) {
            throw new IllegalArgumentException("Le taux patronal doit être entre 0 et 99.");
        }
        return userDAO.updateTauxPatronal(userId, tauxPatronal);
    }

    /**
     * Produit un hash BCrypt d'un mot de passe clair (coût 12).
     *
     * @param password mot de passe en clair
     * @return hash BCrypt
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    private static boolean isBcryptHash(String value) {
        return value != null
                && value.length() == 60
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /**
     * Retourne les identifiants des utilisateurs d'un rôle.
     *
     * @param role nom du rôle
     * @return liste d'identifiants
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Integer> getIdsByRole(String role) throws SQLException {
        List<User> users = getUsersByRole(role);
        return users.stream()
                .map(User::getId)
                .toList();
    }
}
