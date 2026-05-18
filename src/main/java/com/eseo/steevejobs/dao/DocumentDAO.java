package com.eseo.steevejobs.dao;

import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Tiers;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Enum.DocumentType;
import com.eseo.steevejobs.model.Enum.DocumentStatut;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object dédié aux opérations sur la table des documents.
 * <p>
 * Contient les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) permettant de lire
 * et sauvegarder les objets {@link com.eseo.steevejobs.model.Document} en base de données.
 * </p>
 */
public class DocumentDAO {

    /**
     * Créer un nouveau document
     *
     * @param document le document à créer
     * @throws SQLException exception SQL
     */
    public void createDocument(Document document) throws SQLException {
        String sql = "INSERT INTO DOCUMENTS (type, date, total_ht, total_ttc, statut, url, id_tiers, id_vendeur) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, document.getType().getValeur());
            stmt.setDate(2, java.sql.Date.valueOf(document.getDate().toLocalDate()));
            stmt.setBigDecimal(3, document.getPrixHt());
            stmt.setBigDecimal(4, document.getPrixTtc());
            stmt.setString(5, document.getStatut().getValeur());
            stmt.setString(6, document.getUrl());
            stmt.setInt(7, document.getTiers().getId());

            if (document.getEditeur() != null) {
                stmt.setInt(8, document.getEditeur().getId());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    document.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Mettre à jour un document existant
     *
     * @param document le document à mettre à jour
     * @return true si mis à jour, false sinon
     * @throws SQLException exception SQL
     */
    public boolean updateDocument(Document document) throws SQLException {
        String sql = "UPDATE DOCUMENTS SET type = ?, date = ?, total_ht = ?, total_ttc = ?, statut = ?, url = ?, id_tiers = ?, id_vendeur = ? WHERE id_documents = ?";

        int rowsAffected;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, document.getType().getValeur());
            stmt.setDate(2, java.sql.Date.valueOf(document.getDate().toLocalDate()));
            stmt.setBigDecimal(3, document.getPrixHt());
            stmt.setBigDecimal(4, document.getPrixTtc());
            stmt.setString(5, document.getStatut().getValeur());
            stmt.setString(6, document.getUrl());
            stmt.setInt(7, document.getTiers().getId());

            if (document.getEditeur() != null) {
                stmt.setInt(8, document.getEditeur().getId());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }

            stmt.setInt(9, document.getId());

            rowsAffected = stmt.executeUpdate();
        }
        return rowsAffected > 0;
    }

    public boolean updateUrl(int id, String url) throws SQLException {
        String sql = "UPDATE DOCUMENTS SET url = ? WHERE id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, url);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Supprimer un document par son ID
     * @param id l'ID du document
     * @return true si supprimé, false sinon
     * @throws SQLException exception SQL
     */
    public boolean deleteDocument(int id) throws SQLException {
        String sql = "DELETE FROM DOCUMENTS WHERE id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Récupérer un document par son ID
     * @param id l'ID du document
     * @return le document trouvé, null sinon
     * @throws SQLException exception SQL
     */
    public Document getById(int id) throws SQLException {
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "WHERE d.id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Tiers tiers = null;
                    if (rs.getObject("id_tiers") != null) {
                        tiers = new Tiers(
                                rs.getInt("id_tiers"),
                                rs.getString("tiers_nom"),
                                rs.getString("tiers_prenom"),
                                com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                                rs.getString("tiers_email"),
                                rs.getString("tiers_adresse"),
                                rs.getString("tiers_tel"),
                                rs.getString("siret"),
                                rs.getString("num_tva")
                        );
                        tiers.setActif(rs.getBoolean("tiers_actif"));
                    }

                    User editeur = null;
                    if (rs.getObject("id_user") != null) {
                        editeur = new User(
                                rs.getInt("id_user"),
                                rs.getString("user_nom"),
                                rs.getString("user_prenom"),
                                rs.getString("user_email"),
                                rs.getString("mdp"),
                                rs.getString("user_adresse"),
                                rs.getString("role"),
                                rs.getString("user_tel"),
                                rs.getString("poste"),
                                rs.getBoolean("user_actif")
                        );
                    }

                    return new Document(
                            rs.getInt("id_documents"),
                            DocumentType.fromValeur(rs.getString("type")),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getBigDecimal("total_ht"),
                            rs.getBigDecimal("total_ttc"),
                            DocumentStatut.fromValeur(rs.getString("statut")),
                            rs.getString("url"),
                            tiers,
                            editeur
                    );
                }
                return null;
            }
        }
    }

    /**
     * Récupérer tous les documents d'un tiers
     * @param tiersId l'ID du tiers
     * @return la liste des documents du tiers
     * @throws SQLException exception SQL
     */
    public List<Document> findByTiersId(int tiersId) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "WHERE d.id_tiers = ? " +
                "ORDER BY d.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, tiersId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tiers tiers = null;
                    if (rs.getObject("id_tiers") != null) {
                        tiers = new Tiers(
                                rs.getInt("id_tiers"),
                                rs.getString("tiers_nom"),
                                rs.getString("tiers_prenom"),
                                com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                                rs.getString("tiers_email"),
                                rs.getString("tiers_adresse"),
                                rs.getString("tiers_tel"),
                                rs.getString("siret"),
                                rs.getString("num_tva")
                        );
                        tiers.setActif(rs.getBoolean("tiers_actif"));
                    }

                    User editeur = null;
                    if (rs.getObject("id_user") != null) {
                        editeur = new User(
                                rs.getInt("id_user"),
                                rs.getString("user_nom"),
                                rs.getString("user_prenom"),
                                rs.getString("user_email"),
                                rs.getString("mdp"),
                                rs.getString("user_adresse"),
                                rs.getString("role"),
                                rs.getString("user_tel"),
                                rs.getString("poste"),
                                rs.getBoolean("user_actif")
                        );
                    }

                    documents.add(new Document(
                            rs.getInt("id_documents"),
                            DocumentType.fromValeur(rs.getString("type")),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getBigDecimal("total_ht"),
                            rs.getBigDecimal("total_ttc"),
                            DocumentStatut.fromValeur(rs.getString("statut")),
                            rs.getString("url"),
                            tiers,
                            editeur
                    ));
                }
            }
        }
        return documents;
    }

    /**
     * Récupérer les documents par statut
     * @param statut le statut du document
     * @return la liste des documents correspondants
     * @throws SQLException exception SQL
     */
    public List<Document> findByStatut(DocumentStatut statut) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "WHERE d.statut = ? " +
                "ORDER BY d.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut.getValeur());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tiers tiers = null;
                    if (rs.getObject("id_tiers") != null) {
                        tiers = new Tiers(
                                rs.getInt("id_tiers"),
                                rs.getString("tiers_nom"),
                                rs.getString("tiers_prenom"),
                                com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                                rs.getString("tiers_email"),
                                rs.getString("tiers_adresse"),
                                rs.getString("tiers_tel"),
                                rs.getString("siret"),
                                rs.getString("num_tva")
                        );
                        tiers.setActif(rs.getBoolean("tiers_actif"));
                    }

                    User editeur = null;
                    if (rs.getObject("id_user") != null) {
                        editeur = new User(
                                rs.getInt("id_user"),
                                rs.getString("user_nom"),
                                rs.getString("user_prenom"),
                                rs.getString("user_email"),
                                rs.getString("mdp"),
                                rs.getString("user_adresse"),
                                rs.getString("role"),
                                rs.getString("user_tel"),
                                rs.getString("poste"),
                                rs.getBoolean("user_actif")
                        );
                    }

                    documents.add(new Document(
                            rs.getInt("id_documents"),
                            DocumentType.fromValeur(rs.getString("type")),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getBigDecimal("total_ht"),
                            rs.getBigDecimal("total_ttc"),
                            DocumentStatut.fromValeur(rs.getString("statut")),
                            rs.getString("url"),
                            tiers,
                            editeur
                    ));
                }
            }
        }
        return documents;
    }

    /**
     * Récupérer les documents par type
     * @param type le type de document
     * @return la liste des documents correspondants
     * @throws SQLException exception SQL
     */
    public List<Document> findByType(DocumentType type) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "WHERE d.type = ? " +
                "ORDER BY d.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.getValeur());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tiers tiers = null;
                    if (rs.getObject("id_tiers") != null) {
                        tiers = new Tiers(
                                rs.getInt("id_tiers"),
                                rs.getString("tiers_nom"),
                                rs.getString("tiers_prenom"),
                                com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                                rs.getString("tiers_email"),
                                rs.getString("tiers_adresse"),
                                rs.getString("tiers_tel"),
                                rs.getString("siret"),
                                rs.getString("num_tva")
                        );
                        tiers.setActif(rs.getBoolean("tiers_actif"));
                    }

                    User editeur = null;
                    if (rs.getObject("id_user") != null) {
                        editeur = new User(
                                rs.getInt("id_user"),
                                rs.getString("user_nom"),
                                rs.getString("user_prenom"),
                                rs.getString("user_email"),
                                rs.getString("mdp"),
                                rs.getString("user_adresse"),
                                rs.getString("role"),
                                rs.getString("user_tel"),
                                rs.getString("poste"),
                                rs.getBoolean("user_actif")
                        );
                    }

                    documents.add(new Document(
                            rs.getInt("id_documents"),
                            DocumentType.fromValeur(rs.getString("type")),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getBigDecimal("total_ht"),
                            rs.getBigDecimal("total_ttc"),
                            DocumentStatut.fromValeur(rs.getString("statut")),
                            rs.getString("url"),
                            tiers,
                            editeur
                    ));
                }
            }
        }
        return documents;
    }

    /**
     * Récupérer les documents d'un éditeur
     * @param editeurId l'ID de l'éditeur
     * @return la liste des documents de l'éditeur
     * @throws SQLException exception SQL
     */
    public List<Document> findByEditeurId(int editeurId) throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "WHERE d.id_vendeur = ? " +
                "ORDER BY d.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, editeurId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tiers tiers = null;
                    if (rs.getObject("id_tiers") != null) {
                        tiers = new Tiers(
                                rs.getInt("id_tiers"),
                                rs.getString("tiers_nom"),
                                rs.getString("tiers_prenom"),
                                com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                                rs.getString("tiers_email"),
                                rs.getString("tiers_adresse"),
                                rs.getString("tiers_tel"),
                                rs.getString("siret"),
                                rs.getString("num_tva")
                        );
                        tiers.setActif(rs.getBoolean("tiers_actif"));
                    }

                    User editeur = null;
                    if (rs.getObject("id_user") != null) {
                        editeur = new User(
                                rs.getInt("id_user"),
                                rs.getString("user_nom"),
                                rs.getString("user_prenom"),
                                rs.getString("user_email"),
                                rs.getString("mdp"),
                                rs.getString("user_adresse"),
                                rs.getString("role"),
                                rs.getString("user_tel"),
                                rs.getString("poste"),
                                rs.getBoolean("user_actif")
                        );
                    }

                    documents.add(new Document(
                            rs.getInt("id_documents"),
                            DocumentType.fromValeur(rs.getString("type")),
                            rs.getDate("date").toLocalDate().atStartOfDay(),
                            rs.getBigDecimal("total_ht"),
                            rs.getBigDecimal("total_ttc"),
                            DocumentStatut.fromValeur(rs.getString("statut")),
                            rs.getString("url"),
                            tiers,
                            editeur
                    ));
                }
            }
        }
        return documents;
    }

    /**
     * Récupérer tous les documents
     * @return la liste de tous les documents
     * @throws SQLException exception SQL
     */
    public List<Document> findAll() throws SQLException {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT d.*, " +
                "t.id_tiers, t.nom as tiers_nom, t.prenom as tiers_prenom, t.type as tiers_type, t.email as tiers_email, t.adresse as tiers_adresse, t.tel as tiers_tel, t.siret, t.num_tva, t.actif as tiers_actif, " +
                "u.id_user, u.nom as user_nom, u.prenom as user_prenom, u.email as user_email, u.mdp, u.adresse as user_adresse, u.tel as user_tel, u.role, u.poste, u.actif as user_actif " +
                "FROM DOCUMENTS d " +
                "LEFT JOIN TIERS t ON d.id_tiers = t.id_tiers " +
                "LEFT JOIN USER u ON d.id_vendeur = u.id_user " +
                "ORDER BY d.date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Tiers tiers = null;
                if (rs.getObject("id_tiers") != null) {
                    tiers = new Tiers(
                            rs.getInt("id_tiers"),
                            rs.getString("tiers_nom"),
                            rs.getString("tiers_prenom"),
                            com.eseo.steevejobs.model.Enum.TiersType.valueOf(rs.getString("tiers_type").toUpperCase()),
                            rs.getString("tiers_email"),
                            rs.getString("tiers_adresse"),
                            rs.getString("tiers_tel"),
                            rs.getString("siret"),
                            rs.getString("num_tva")
                    );
                    tiers.setActif(rs.getBoolean("tiers_actif"));
                }

                User editeur = null;
                if (rs.getObject("id_user") != null) {
                    editeur = new User(
                            rs.getInt("id_user"),
                            rs.getString("user_nom"),
                            rs.getString("user_prenom"),
                            rs.getString("user_email"),
                            rs.getString("mdp"),
                            rs.getString("user_adresse"),
                            rs.getString("role"),
                            rs.getString("user_tel"),
                            rs.getString("poste"),
                            rs.getBoolean("user_actif")
                    );
                }

                documents.add(new Document(
                        rs.getInt("id_documents"),
                        DocumentType.fromValeur(rs.getString("type")),
                        rs.getDate("date").toLocalDate().atStartOfDay(),
                        rs.getBigDecimal("total_ht"),
                        rs.getBigDecimal("total_ttc"),
                        DocumentStatut.fromValeur(rs.getString("statut")),
                        rs.getString("url"),
                        tiers,
                        editeur
                ));
            }
        }
        return documents;
    }

    /**
     * Mettre à jour le statut d'un document
     * @param id     l'ID du document
     * @param statut le nouveau statut
     * @return true si mis à jour, false sinon
     * @throws SQLException exception SQL
     */
    public boolean updateStatut(int id, DocumentStatut statut) throws SQLException {
        String sql = "UPDATE DOCUMENTS SET statut = ? WHERE id_documents = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.getValeur());
            stmt.setInt(2, id);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Compter le nombre total de documents
     * @return le nombre total de documents
     * @throws SQLException exception SQL
     */
    public int countDocuments() throws SQLException {
        String sql = "SELECT COUNT(*) FROM DOCUMENTS";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de documents par statut
     * @param statut le statut
     * @return le nombre de documents avec ce statut
     * @throws SQLException exception SQL
     */
    public int countByStatut(DocumentStatut statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DOCUMENTS WHERE statut = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.getValeur()); 

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Compter le nombre de documents par type
     * @param type le type
     * @return le nombre de documents avec ce type
     * @throws SQLException exception SQL
     */
    public int countByType(DocumentType type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM DOCUMENTS WHERE type = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.getValeur());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}