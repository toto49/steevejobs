package com.eseo.steevejobs.service;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestion des documents commerciaux (devis, factures, bons de commande) et de leurs lignes.
 * <p>
 * Règles métier : tiers et montants obligatoires ; date non future ; TTC ≥ HT ;
 * lignes avec produit, quantité et prix valides. Effets de bord :
 * génération PDF locale via {@link PdfGeneratorService} ;
 * suppression asynchrone du fichier sur le NAS ({@link WebDavService}) après suppression BDD.
 * </p>
 */
public class DocumentService {

    private final DocumentDAO         documentDAO;
    private final ComposerDAO         composerDAO;
    private final PdfGeneratorService pdfService;

    /**
     * Constructeur avec DAO document et dépendances par défaut.
     *
     * @param documentDAO accès persistance des documents
     */
    public DocumentService(DocumentDAO documentDAO) {
        this(documentDAO, new ComposerDAO(), new PdfGeneratorService());
    }

    /**
     * Constructeur avec injection complète (tests).
     *
     * @param documentDAO accès documents
     * @param composerDAO accès lignes
     * @param pdfService  générateur PDF
     */
    public DocumentService(DocumentDAO documentDAO, ComposerDAO composerDAO,
                           PdfGeneratorService pdfService) {
        this.documentDAO = documentDAO;
        this.composerDAO = composerDAO;
        this.pdfService  = pdfService;
    }

    /**
     * Constructeur par défaut.
     */
    public DocumentService() {
        this(new DocumentDAO());
    }

    /**
     * Charge un document par identifiant.
     *
     * @param idDocument identifiant strictement positif
     * @return document ou {@code null} selon le DAO
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public Document getDocumentById(int idDocument) throws SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }
        return documentDAO.getById(idDocument);
    }

    /**
     * Liste les lignes de composition d'un document.
     *
     * @param idDocument identifiant du document
     * @return lignes produit associées
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public List<Composer> findLignesByDocumentId(int idDocument) throws SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }
        return composerDAO.findByDocumentId(idDocument);
    }

    /**
     * Crée un document et ses lignes après validation.
     *
     * @param document entête document (identifiant renseigné après création)
     * @param lignes   lignes de détail
     * @throws IllegalArgumentException si données invalides
     * @throws SQLException             en cas d'erreur d'accès base
     */
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

    /**
     * Met à jour l'entête document sans toucher aux lignes.
     *
     * @param document document avec identifiant valide
     * @throws IllegalArgumentException si données invalides
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la mise à jour BDD échoue
     */
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

    /**
     * Met à jour l'entête puis remplace l'ensemble des lignes (suppression puis recréation).
     *
     * @param document document à modifier
     * @param lignes   nouvelles lignes
     * @throws IllegalArgumentException si données invalides
     * @throws SQLException             en cas d'erreur d'accès base
     */
    public void modifierDocumentAvecLignes(Document document, List<Composer> lignes)
            throws IllegalArgumentException, SQLException {
        modifierDocument(document);
        validerLignes(lignes);
        composerDAO.deleteByDocumentId(document.getId());
        for (Composer ligne : lignes) {
            ligne.setIdDocument(document.getId());
            composerDAO.createLigne(ligne);
        }
    }

    /**
     * Supprime le document en base puis déclenche la suppression du PDF sur le NAS (asynchrone).
     *
     * @param idDocument identifiant du document
     * @throws IllegalArgumentException si l'identifiant est invalide
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la suppression BDD échoue
     */
    public void supprimerDocument(int idDocument) throws IllegalArgumentException, SQLException {
        if (idDocument <= 0) {
            throw new IllegalArgumentException("L'ID du document est invalide.");
        }

        Document doc = documentDAO.getById(idDocument);
        if (doc == null) {
            return;
        }

        String nomFichier = String.format("%s_%d.pdf",
                doc.getType().getValeur().replace(" ", "_"), doc.getId());

        boolean success = documentDAO.deleteDocument(idDocument);
        if (!success) {
            throw new RuntimeException("Erreur BDD : Impossible de supprimer ce document.");
        }

        CompletableFuture.runAsync(() ->
                WebDavService.supprimerFichierDuNAS("documents_commerciaux", nomFichier));
    }

    /**
     * Génère le PDF du document, met à jour l'URL en base et retourne le chemin local.
     *
     * @param idDocument identifiant du document
     * @return chemin ou URL du fichier PDF généré
     * @throws IllegalArgumentException si document introuvable
     * @throws SQLException             en cas d'erreur d'accès base
     * @throws RuntimeException         si la génération PDF échoue
     */
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

    /**
     * Liste tous les documents.
     *
     * @return liste complète
     * @throws SQLException en cas d'erreur d'accès base
     */
    public List<Document> findAll() throws SQLException {
        return documentDAO.findAll();
    }

    /**
     * Met à jour le statut workflow d'un document.
     *
     * @param id     identifiant du document
     * @param statut nouveau statut
     * @return {@code true} si la mise à jour a réussi
     * @throws SQLException en cas d'erreur d'accès base
     */
    public boolean updateStatut(int id, DocumentStatut statut) throws SQLException {
        return documentDAO.updateStatut(id, statut);
    }

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
            throw new IllegalArgumentException("La date du document ne peut pas être dans le futur.");
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
            throw new IllegalArgumentException("Le montant TTC ne peut pas être inférieur au montant HT.");
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
