package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
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

    // --------------------------------------------------------
    // MÉTHODES PUBLIQUES
    // --------------------------------------------------------

    public void ajouterDocument(Document document, List<Composer> lignes)
            throws IllegalArgumentException, SQLException {

        validerDocument(document);
        validerLignes(lignes);

        documentDAO.createDocument(document);

        for (Composer ligne : lignes) {
            ligne.setIdDocument(document.getId());
            composerDAO.createLigne(ligne);
        }
    }

    public void modifierDocument(Document document)
            throws IllegalArgumentException, SQLException {

        if (document.getId() <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide pour une modification.");
        }

        validerDocument(document);

        boolean success = documentDAO.updateDocument(document);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de mettre à jour ce document.");
        }
    }

    public void supprimerDocument(int idDocument) throws IllegalArgumentException, SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }

        boolean success = documentDAO.deleteDocument(idDocument);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce document.");
        }
    }

    public String exporterPdf(int idDocument) throws IllegalArgumentException, SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }

        Document document = documentDAO.getById(idDocument);
        if (document == null) {
            throw new IllegalArgumentException("Document introuvable pour l'ID : " + idDocument);
        }

        List<Composer> lignes = composerDAO.findByDocumentId(idDocument);
        String url = pdfService.genererDocument(document, lignes);
        documentDAO.updateUrl(idDocument, url);
        document.setUrl(url);

        return url;
    }

    public void regenererPdf(int idDocument) throws IllegalArgumentException, SQLException {
        exporterPdf(idDocument);
    }

    public boolean changerStatut(int idDocument, DocumentStatut nouveauStatut)
            throws IllegalArgumentException, SQLException {

        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le nouveau statut est obligatoire.");
        }

        return documentDAO.updateStatut(idDocument, nouveauStatut);
    }

    public Document getDocumentById(int id) throws IllegalArgumentException, SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }
        return documentDAO.getById(id);
    }

    public List<Document> obtenirTousLesDocuments() throws SQLException {
        return documentDAO.findAll();
    }

    public List<Document> getByTiersId(int tiersId) throws IllegalArgumentException, SQLException {
        if (tiersId <= 0) {
            throw new IllegalArgumentException("L'ID du tiers est invalide.");
        }
        return documentDAO.findByTiersId(tiersId);
    }

    public List<Composer> getLignes(int idDocument) throws IllegalArgumentException, SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }
        return composerDAO.findByDocumentId(idDocument);
    }

    // Méthodes conservées pour rétrocompatibilité
    public List<Document> findAll()    throws SQLException { return documentDAO.findAll(); }
    public boolean updateStatut(int id, DocumentStatut statut) throws SQLException {
        return documentDAO.updateStatut(id, statut);
    }

    // --------------------------------------------------------
    // MÉTHODES PRIVÉES (Logique métier interne)
    // --------------------------------------------------------

    private void validerDocument(Document document) throws IllegalArgumentException {
        if (document == null) {
            throw new IllegalArgumentException("Les données du document sont vides.");
        }

        if (document.getType() == null) {
            throw new IllegalArgumentException("Le type du document est obligatoire.");
        }

        if (document.getTiers() == null || document.getTiers().getId() <= 0) {
            throw new IllegalArgumentException("Le tiers (client) associé au document est obligatoire.");
        }

        if (document.getDate() == null) {
            throw new IllegalArgumentException("La date du document est obligatoire.");
        }

        if (document.getDate().isAfter(LocalDateTime.now().plusDays(1))) {
            throw new IllegalArgumentException(
                    "La date du document ne peut pas être dans le futur.");
        }

        if (document.getStatut() == null) {
            throw new IllegalArgumentException("Le statut du document est obligatoire.");
        }

        if (document.getPrixHt() == null) {
            throw new IllegalArgumentException("Le montant HT est obligatoire.");
        }

        if (document.getPrixHt().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le montant HT ne peut pas être négatif.");
        }

        if (document.getPrixTtc() == null) {
            throw new IllegalArgumentException("Le montant TTC est obligatoire.");
        }

        if (document.getPrixTtc().compareTo(document.getPrixHt()) < 0) {
            throw new IllegalArgumentException(
                    "Le montant TTC ne peut pas être inférieur au montant HT.");
        }
    }

    private void validerLignes(List<Composer> lignes) throws IllegalArgumentException {
        if (lignes == null) {
            throw new IllegalArgumentException("La liste des lignes produits est nulle.");
        }

        for (int i = 0; i < lignes.size(); i++) {
            Composer ligne = lignes.get(i);

            if (ligne.getProduit() == null || ligne.getProduit().getId() <= 0) {
                throw new IllegalArgumentException(
                        "La ligne " + (i + 1) + " ne contient pas de produit valide.");
            }

            if (ligne.getQuantite() == null
                    || ligne.getQuantite().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "La quantité de la ligne " + (i + 1) + " doit être supérieure à 0.");
            }

            if (ligne.getPrixVente() == null
                    || ligne.getPrixVente().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Le prix de vente de la ligne " + (i + 1) + " ne peut pas être négatif.");
            }
        }
    }
}
