package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Planning;

import java.sql.SQLException;
import java.util.List;

public class DocumentService {

    private final DocumentDAO documentDAO;


    public DocumentService(DocumentDAO documentDAO) {this.documentDAO = documentDAO;}

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES (Appelées par tes contrôleurs JavaFX)
    // --------------------------------------------------------

    public void ajouterDocument(Document document) throws SQLException {

        validerDocument(document);

        boolean success = documentDAO.createDocument(document);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible d'ajouter ce document.");
        }
    }

    public void modifierDocument(Document document) throws SQLException {

        validerDocument(document);

        boolean success = documentDAO.updateDocument(document);

    }

    public void supprimerDocument(int idDocument) throws SQLException {

        boolean success = documentDAO.deleteDocument(idDocument);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce document.");
        }
    }

    public List<Document> getByTiersId(int tiersId) throws SQLException {
        return documentDAO.findByTiersId(tiersId);
    }

    //TODO : estPayé /

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerDocument(Document document) {

        if (document.getType() == null) {
            throw new IllegalArgumentException("Le type de document ne peut pas être nul.");
        }

        if (document.getTiers() == null){
            throw new IllegalArgumentException("Le tiers est obligatoire.");
        }

        if (document.getDate() == null){
            throw new IllegalArgumentException("La date est obligatoire.");
        }

    }
}
