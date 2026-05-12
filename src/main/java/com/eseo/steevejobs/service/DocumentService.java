package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;

import java.sql.SQLException;
import java.util.List;

public class DocumentService {

    private final DocumentDAO         documentDAO;
    private final ComposerDAO         composerDAO;
    private final PdfGeneratorService pdfService;

    public DocumentService(DocumentDAO documentDAO) {
        this.documentDAO = documentDAO;
        this.composerDAO = new ComposerDAO();
        this.pdfService  = new PdfGeneratorService();
    }

    /**
     * Crée un document sans générer de PDF.
     * @param document  le document à créer
     * @param lignes    les lignes produits (peut être vide si saisie ultérieure)
     */
    public void ajouterDocument(Document document, List<Composer> lignes) throws SQLException {
        validerDocument(document);

        documentDAO.createDocument(document);

        // Sauvegarder les lignes produits
        for (Composer ligne : lignes) {
            ligne.setIdDocument(document.getId());
            composerDAO.createLigne(ligne);
        }
    }

    /**
     * Exporte un document en PDF (génère le PDF et met à jour l'URL).
     * @param idDocument l'ID du document
     * @return l'URL du PDF généré
     */
    public String exporterPdf(int idDocument) throws SQLException {
        Document document = documentDAO.getById(idDocument);
        if (document == null) throw new IllegalArgumentException("Document introuvable.");

        List<Composer> lignes = composerDAO.findByDocumentId(idDocument);
        String url = pdfService.genererDocument(document, lignes);
        documentDAO.updateUrl(idDocument, url);
        document.setUrl(url);

        return url;
    }

    /**
     * Régénère le PDF d'un document existant (après modification).
     */
    public void regenererPdf(int idDocument) throws SQLException {
        exporterPdf(idDocument); // Réutilise la même méthode
    }

    public void modifierDocument(Document document) throws SQLException {
        validerDocument(document);
        documentDAO.updateDocument(document);
    }

    public void supprimerDocument(int idDocument) throws SQLException {
        boolean success = documentDAO.deleteDocument(idDocument);
        if (!success) throw new RuntimeException("Erreur BDD : Impossible de supprimer ce document.");
    }

    public List<Document> getByTiersId(int tiersId) throws SQLException {
        return documentDAO.findByTiersId(tiersId);
    }

    public List<Composer> getLignes(int idDocument) throws SQLException {
        return composerDAO.findByDocumentId(idDocument);
    }

    public List<Document> findAll() throws SQLException {
        return documentDAO.findAll();
    }

    public boolean updateStatut(int id, DocumentStatut statut) throws SQLException {
        return documentDAO.updateStatut(id, statut);
    }

    private void validerDocument(Document document) {
        if (document.getType()   == null) throw new IllegalArgumentException("Le type est obligatoire.");
        if (document.getTiers()  == null) throw new IllegalArgumentException("Le tiers est obligatoire.");
        if (document.getDate()   == null) throw new IllegalArgumentException("La date est obligatoire.");
    }
}