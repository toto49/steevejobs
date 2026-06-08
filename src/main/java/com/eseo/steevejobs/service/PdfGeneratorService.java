package com.eseo.steevejobs.service;

import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Génération locale de PDF (documents commerciaux et bulletins de paie) via OpenPDF.
 * <p>
 * Effet de bord fichier : écriture dans le répertoire {@code Downloads} de l'utilisateur
 * système ({@code user.home}). Aucun envoi réseau ; le chemin absolu retourné sert
 * d'URL stockée en base. Les méthodes privées construisent la mise en page.
 * </p>
 */
public class PdfGeneratorService {

    /** Répertoire de sortie des PDF générés ({@code Downloads} de l'utilisateur courant). */
    private static final String OUTPUT_DIR = System.getProperty("user.home") + File.separator + "Downloads" + File.separator;

    /** Couleur d'accentuation des en-têtes et titres de section. */
    private static final Color COULEUR_PRINCIPALE = new Color(75, 120, 204);
    /** Couleur des libellés secondaires et mentions discrètes. */
    private static final Color COULEUR_GRIS       = new Color(107, 114, 128);
    /** Couleur de fond des lignes alternées des tableaux. */
    private static final Color COULEUR_FOND_LIGNE = new Color(244, 245, 247);
    /** Couleur des bordures de cellules et séparateurs. */
    private static final Color COULEUR_BORDURE    = new Color(209, 213, 219);
    /** Couleur du corps de texte principal. */
    private static final Color COULEUR_TEXTE      = new Color(50, 50, 50);

    /**
     * Génère le PDF d'un document commercial (devis, facture ou bon de commande).
     *
     * @param document entête document (type, tiers, montants)
     * @param lignes   lignes de détail produit
     * @return chemin absolu du fichier PDF créé sous {@code Downloads}
     * @throws RuntimeException si la génération ou l'écriture disque échoue
     */
    public String genererDocument(com.eseo.steevejobs.model.Document document, List<Composer> lignes) {
        creerDossier();
        String nomFichier = String.format("%s_%d.pdf",
                document.getType().getValeur().replace(" ", "_"),
                document.getId());
        String chemin = OUTPUT_DIR + nomFichier;

        try (FileOutputStream fos = new FileOutputStream(chemin)) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4, 55, 55, 60, 60);
            PdfWriter.getInstance(doc, fos);
            doc.open();
            ajouterContenuDocument(doc, document, lignes);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF document : " + e.getMessage(), e);
        }
        return chemin;
    }

    /**
     * Génère le bulletin de paie PDF avec déduction congés et cotisations.
     *
     * @param fiche                       fiche persistée (employé, période)
     * @param salaireBrut                 salaire brut de référence
     * @param tauxCotisationsPatronales   taux patronal (0–1)
     * @param joursConge                  jours de congé sur le mois (planning)
     * @param heuresTravaillees           heures travaillées affichées
     * @param tauxHoraire                 taux horaire affiché
     * @return chemin absolu du PDF sous {@code Downloads}
     * @throws RuntimeException si la génération ou l'écriture disque échoue
     */
    public String genererFichePaye(FichePaye fiche, double salaireBrut,
                                   double tauxCotisationsPatronales, long joursConge,
                                   double heuresTravaillees, double tauxHoraire) {
        creerDossier();
        String nomFichier = String.format("fiche_%d_%d_%02d.pdf",
                fiche.getEmploye().getId(),
                fiche.getDate().getYear(),
                fiche.getDate().getMonthValue());
        String chemin = OUTPUT_DIR + nomFichier;

        try (FileOutputStream fos = new FileOutputStream(chemin)) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4, 55, 55, 60, 60);
            PdfWriter.getInstance(doc, fos);
            doc.open();
            ajouterContenuFichePaye(doc, fiche, salaireBrut, tauxCotisationsPatronales,
                    joursConge, heuresTravaillees, tauxHoraire);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF fiche de paie : " + e.getMessage(), e);
        }
        return chemin;
    }

    /**
     * Charge le logo applicatif depuis les ressources classpath.
     *
     * @return image redimensionnée pour l'en-tête PDF, ou {@code null} si indisponible
     */
    private Image chargerLogo() {
        try {
            InputStream is = getClass().getResourceAsStream("/images/logo.png");
            if (is == null) return null;
            Image logo = Image.getInstance(is.readAllBytes());
            logo.scaleToFit(80, 80);
            return logo;
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------
    // CONTENU — DOCUMENT (devis, facture, bon de commande)
    // -------------------------------------------------------

    /**
     * Assemble le contenu PDF d'un document commercial (en-tête, client, lignes, totaux).
     *
     * @param doc      document OpenPDF ouvert
     * @param document entête métier du document
     * @param lignes   lignes de détail produit
     * @throws DocumentException en cas d'erreur de composition PDF
     */
    private void ajouterContenuDocument(com.lowagie.text.Document doc,
                                        com.eseo.steevejobs.model.Document document,
                                        List<Composer> lignes) throws DocumentException {
        Image logo = chargerLogo();
        if (logo != null) {
            logo.setAlignment(Element.ALIGN_RIGHT);
            logo.setSpacingAfter(5);
            doc.add(logo);
        }

        Font fontTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COULEUR_PRINCIPALE);
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COULEUR_PRINCIPALE);
        Font fontNormal  = FontFactory.getFont(FontFactory.HELVETICA, 11, COULEUR_TEXTE);
        Font fontBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COULEUR_TEXTE);
        Font fontGris    = FontFactory.getFont(FontFactory.HELVETICA, 10, COULEUR_GRIS);
        Font fontTotal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COULEUR_PRINCIPALE);

        String typeLabel = switch (document.getType()) {
            case DEVIS -> "DEVIS";
            case FACTURE -> "FACTURE";
            case BON_COMMANDE -> "BON DE COMMANDE";
        };

        Paragraph titre = new Paragraph(typeLabel, fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);

        Paragraph ref = new Paragraph("N° " + document.getId() + " — " +
                document.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        ref.setAlignment(Element.ALIGN_CENTER);
        doc.add(ref);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // CLIENT
        doc.add(new Paragraph("CLIENT", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableInfos = new PdfPTable(2);
        tableInfos.setWidthPercentage(100);
        tableInfos.setWidths(new float[]{1, 3});
        tableInfos.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        tableInfos.getDefaultCell().setPadding(2);

        ajouterInfoLigne(tableInfos, "Nom", document.getTiers().getNom(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Email", document.getTiers().getEmail(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Adresse", document.getTiers().getAdresse(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Tél", document.getTiers().getTel(), fontGris, fontNormal);
        doc.add(tableInfos);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // VENDEUR
        doc.add(new Paragraph("VENDEUR", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableVendeur = new PdfPTable(2);
        tableVendeur.setWidthPercentage(100);
        tableVendeur.setWidths(new float[]{1, 3});
        tableVendeur.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        tableVendeur.getDefaultCell().setPadding(2);

        User vendeur = document.getEditeur();
        if (vendeur != null) {
            String nomComplet = (vendeur.getPrenom() != null ? vendeur.getPrenom() : "") + " " + (vendeur.getNom() != null ? vendeur.getNom() : "");
            ajouterInfoLigne(tableVendeur, "Nom", nomComplet.trim().isEmpty() ? "Non renseigné" : nomComplet, fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Poste", vendeur.getPoste() != null ? vendeur.getPoste() : "Non renseigné", fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Email", vendeur.getEmail() != null ? vendeur.getEmail() : "Non renseigné", fontGris, fontNormal);
        }
        doc.add(tableVendeur);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // DÉTAIL DES PRESTATIONS
        doc.add(new Paragraph("DÉTAIL DES PRESTATIONS", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableLignes = new PdfPTable(5);
        tableLignes.setWidthPercentage(100);
        tableLignes.setWidths(new float[]{4, 1.5f, 2, 1.5f, 2});

        for (String entete : new String[]{"Désignation", "Qté", "Prix unit. HT", "TVA", "Total HT"}) {
            PdfPCell cell = new PdfPCell(new Phrase(entete, fontBold));
            cell.setBackgroundColor(COULEUR_FOND_LIGNE);
            cell.setPadding(5);
            cell.setBorderColor(COULEUR_BORDURE);
            tableLignes.addCell(cell);
        }

        boolean alterne = false;
        for (Composer ligne : lignes) {
            Color fond = alterne ? COULEUR_FOND_LIGNE : Color.WHITE;
            BigDecimal totalHt = ligne.getPrixVente().multiply(ligne.getQuantite());
            ajouterCelluleLigne(tableLignes, ligne.getProduit().getNom(), fontNormal, fond, Element.ALIGN_LEFT);
            ajouterCelluleLigne(tableLignes, ligne.getQuantite().stripTrailingZeros().toPlainString(), fontNormal, fond, Element.ALIGN_CENTER);
            ajouterCelluleLigne(tableLignes, String.format("%.2f €", ligne.getPrixVente()), fontNormal, fond, Element.ALIGN_RIGHT);
            ajouterCelluleLigne(tableLignes, ligne.getProduit().getTauxTva() + " %", fontNormal, fond, Element.ALIGN_CENTER);
            ajouterCelluleLigne(tableLignes, String.format("%.2f €", totalHt), fontNormal, fond, Element.ALIGN_RIGHT);
            alterne = !alterne;
        }
        doc.add(tableLignes);
        doc.add(new Paragraph(" "));

        // TOTAUX
        PdfPTable tableTotaux = new PdfPTable(2);
        tableTotaux.setWidthPercentage(45);
        tableTotaux.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tableTotaux.setWidths(new float[]{2, 2});
        ajouterLigneTotaux(tableTotaux, "Total HT", String.format("%.2f €", document.getPrixHt()), fontNormal);
        ajouterLigneTotaux(tableTotaux, "Total TTC", String.format("%.2f €", document.getPrixTtc()), fontTotal);
        doc.add(tableTotaux);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);

        Paragraph statut = new Paragraph("Statut : " + document.getStatut().name(), fontGris);
        statut.setAlignment(Element.ALIGN_RIGHT);
        doc.add(statut);

        doc.add(new Paragraph(" "));

        Paragraph footer = new Paragraph(
                "Document généré par SteeveJobs — " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // -------------------------------------------------------
    // CONTENU — FICHE DE PAIE
    // -------------------------------------------------------

    /**
     * Assemble le contenu PDF d'un bulletin de paie (employé, rémunération, charges, net).
     *
     * @param doc                         document OpenPDF ouvert
     * @param fiche                       fiche de paie persistée
     * @param salaireBrut                 salaire brut de référence
     * @param tauxCotisationsPatronales   taux patronal appliqué
     * @param joursConge                  jours de congé déduits sur le mois
     * @param heuresTravaillees           heures travaillées affichées
     * @param tauxHoraire                 taux horaire affiché
     * @throws DocumentException en cas d'erreur de composition PDF
     */
    private void ajouterContenuFichePaye(com.lowagie.text.Document doc, FichePaye fiche,
                                         double salaireBrut, double tauxCotisationsPatronales,
                                         long joursConge, double heuresTravaillees, double tauxHoraire) throws DocumentException {

        Image logo = chargerLogo();
        if (logo != null) {
            logo.setAlignment(Element.ALIGN_RIGHT);
            logo.setSpacingAfter(10);
            doc.add(logo);
        }

        Font fontTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COULEUR_PRINCIPALE);
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COULEUR_PRINCIPALE);
        Font fontNormal  = FontFactory.getFont(FontFactory.HELVETICA, 11, COULEUR_TEXTE);
        Font fontBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COULEUR_TEXTE);
        Font fontGris    = FontFactory.getFont(FontFactory.HELVETICA, 10, COULEUR_GRIS);
        Font fontNet     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COULEUR_PRINCIPALE);

        String periode = fiche.getDate()
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));

        Paragraph titre = new Paragraph("BULLETIN DE PAIE", fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);

        Paragraph sousTitre = new Paragraph("Période : " + periode, fontGris);
        sousTitre.setAlignment(Element.ALIGN_CENTER);
        doc.add(sousTitre);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // INFORMATIONS EMPLOYÉ
        doc.add(new Paragraph("INFORMATIONS EMPLOYÉ", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableInfos = new PdfPTable(2);
        tableInfos.setWidthPercentage(100);
        tableInfos.setWidths(new float[]{1, 2});
        tableInfos.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        tableInfos.getDefaultCell().setPadding(4);

        ajouterInfoLigne(tableInfos, "Nom", fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Poste", fiche.getEmploye().getPoste(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Email", fiche.getEmploye().getEmail(), fontGris, fontNormal);
        doc.add(tableInfos);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // DÉTAIL DE LA RÉMUNÉRATION
        doc.add(new Paragraph("DÉTAIL DE LA RÉMUNÉRATION", fontSection));
        doc.add(new Paragraph(" "));

        // Calculs
        double deductionConges = 0;
        double salaireBrutAjuste = salaireBrut;

        if (joursConge > 0) {
            double tauxJournalier = salaireBrut / 22.0;
            deductionConges = tauxJournalier * joursConge;
            salaireBrutAjuste = salaireBrut - deductionConges;
        }

        double cotisationsPatronales = salaireBrutAjuste * tauxCotisationsPatronales;
        double cotisationsRetraite = salaireBrutAjuste * 0.03;
        double totalCharges = cotisationsPatronales + cotisationsRetraite;
        double netAPayer = salaireBrutAjuste - totalCharges;

        PdfPTable tableMontants = new PdfPTable(3);
        tableMontants.setWidthPercentage(100);
        tableMontants.setWidths(new float[]{4, 3.5f, 2.5f});

        for (String entete : new String[]{"Libellé", "Taux / Détail", "Montant"}) {
            PdfPCell cell = new PdfPCell(new Phrase(entete, fontBold));
            cell.setBackgroundColor(COULEUR_FOND_LIGNE);
            cell.setPadding(8);
            cell.setBorderColor(COULEUR_BORDURE);
            tableMontants.addCell(cell);
        }

        // Salaire brut (heures × taux horaire)
        ajouterLigneMontant(tableMontants, "Salaire brut",
                String.format("%.2f h × %.2f €/h", heuresTravaillees, tauxHoraire),
                String.format("%.2f €", salaireBrut), fontNormal, false);

        // Congés (si applicable)
        if (joursConge > 0) {
            ajouterLigneMontant(tableMontants, "Congés (" + joursConge + " jour(s))",
                    String.format("%.2f € / jour", salaireBrut / 22.0),
                    String.format("- %.2f €", deductionConges), fontNormal, true);
        }

        // Salaire brut après congés
        if (joursConge > 0) {
            ajouterLigneMontant(tableMontants, "Salaire brut après congés", "",
                    String.format("%.2f €", salaireBrutAjuste), fontNormal, false);
        }

        // Cotisations patronales
        ajouterLigneMontant(tableMontants, "Cotisations patronales",
                String.format("%.1f %%", tauxCotisationsPatronales * 100),
                String.format("- %.2f €", cotisationsPatronales), fontNormal, true);

        // Cotisations retraite (3%)
        ajouterLigneMontant(tableMontants, "Cotisations retraite", "3%",
                String.format("- %.2f €", cotisationsRetraite), fontNormal, true);

        // Total des charges
        ajouterLigneMontant(tableMontants, "Total des charges", "",
                String.format("- %.2f €", totalCharges), fontNormal, true);

        // NET À PAYER
        PdfPCell cLib = new PdfPCell(new Phrase("NET À PAYER", fontNet));
        cLib.setBorder(Rectangle.TOP);
        cLib.setPadding(10);
        cLib.setBackgroundColor(new Color(240, 248, 255));
        tableMontants.addCell(cLib);

        PdfPCell cDet = new PdfPCell(new Phrase("", fontNet));
        cDet.setBorder(Rectangle.TOP);
        cDet.setPadding(10);
        cDet.setBackgroundColor(new Color(240, 248, 255));
        tableMontants.addCell(cDet);

        PdfPCell cNet = new PdfPCell(new Phrase(String.format("%.2f €", netAPayer), fontNet));
        cNet.setBorder(Rectangle.TOP);
        cNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cNet.setPadding(10);
        cNet.setBackgroundColor(new Color(240, 248, 255));
        tableMontants.addCell(cNet);

        doc.add(tableMontants);

        // Notes
        doc.add(new Paragraph(" "));
        Paragraph note = new Paragraph(
                "ⓘ  Cotisations patronales " + String.format("%.0f", tauxCotisationsPatronales * 100) +
                        "% | Retraite 3% | Net = Brut - charges",
                fontGris);
        note.setAlignment(Element.ALIGN_CENTER);
        doc.add(note);

        if (joursConge > 0) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("ⓘ  " + joursConge + " jour(s) de congé détecté(s) automatiquement depuis le planning.", fontGris));
        }

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);

        Paragraph footer = new Paragraph(
                "Document généré automatiquement par SteeveJobs — " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    /**
     * Crée le répertoire de sortie des PDF s'il n'existe pas encore.
     *
     * @throws RuntimeException si la création du dossier {@link #OUTPUT_DIR} échoue
     */
    private void creerDossier() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier : " + OUTPUT_DIR, e);
        }
    }

    /**
     * Insère une ligne de séparation horizontale dans le document PDF.
     *
     * @param doc document OpenPDF cible
     * @throws DocumentException en cas d'erreur d'ajout au document
     */
    private void ajouterSeparateur(com.lowagie.text.Document doc) throws DocumentException {
        PdfPTable ligne = new PdfPTable(1);
        ligne.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthTop(1f);
        cell.setBorderColorTop(COULEUR_BORDURE);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setFixedHeight(1f);
        ligne.addCell(cell);
        doc.add(ligne);
    }

    /**
     * Ajoute une paire libellé / valeur dans un tableau d'informations.
     *
     * @param table  tableau PDF cible
     * @param label  libellé de la ligne
     * @param valeur texte affiché (chaîne vide si {@code null})
     * @param fLabel police du libellé
     * @param fValeur police de la valeur
     */
    private void ajouterInfoLigne(PdfPTable table, String label, String valeur,
                                  Font fLabel, Font fValeur) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fLabel));
        cL.setBorder(Rectangle.NO_BORDER);
        cL.setPadding(4);
        table.addCell(cL);
        PdfPCell cV = new PdfPCell(new Phrase(valeur != null ? valeur : "", fValeur));
        cV.setBorder(Rectangle.NO_BORDER);
        cV.setPadding(4);
        table.addCell(cV);
    }

    /**
     * Ajoute une cellule de ligne de détail dans un tableau produit.
     *
     * @param table      tableau PDF cible
     * @param texte      contenu de la cellule
     * @param font       police appliquée
     * @param fond       couleur de fond de la ligne
     * @param alignement alignement horizontal ({@link Element#ALIGN_LEFT}, etc.)
     */
    private void ajouterCelluleLigne(PdfPTable table, String texte, Font font,
                                     Color fond, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(fond);
        cell.setPadding(7);
        cell.setBorderColor(COULEUR_BORDURE);
        cell.setHorizontalAlignment(alignement);
        table.addCell(cell);
    }

    /**
     * Ajoute une ligne de montant (libellé, détail, montant) dans le tableau de rémunération.
     *
     * @param table    tableau PDF cible
     * @param libelle  intitulé de la ligne
     * @param detail   complément (taux, formule, etc.)
     * @param montant  montant formaté affiché à droite
     * @param font     police appliquée
     * @param alterne  {@code true} pour appliquer la couleur de fond alternée
     */
    private void ajouterLigneMontant(PdfPTable table, String libelle, String detail,
                                     String montant, Font font, boolean alterne) {
        Color fond = alterne ? COULEUR_FOND_LIGNE : Color.WHITE;
        PdfPCell cL = new PdfPCell(new Phrase(libelle, font));
        cL.setBackgroundColor(fond);
        cL.setPadding(8);
        cL.setBorderColor(COULEUR_BORDURE);
        table.addCell(cL);

        PdfPCell cD = new PdfPCell(new Phrase(detail, font));
        cD.setBackgroundColor(fond);
        cD.setPadding(8);
        cD.setBorderColor(COULEUR_BORDURE);
        table.addCell(cD);

        PdfPCell cM = new PdfPCell(new Phrase(montant, font));
        cM.setBackgroundColor(fond);
        cM.setPadding(8);
        cM.setBorderColor(COULEUR_BORDURE);
        cM.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cM);
    }

    /**
     * Ajoute une ligne de total (libellé et montant) dans un tableau de synthèse.
     *
     * @param table   tableau PDF cible
     * @param label   intitulé du total
     * @param montant montant formaté aligné à droite
     * @param font    police appliquée
     */
    private void ajouterLigneTotaux(PdfPTable table, String label,
                                    String montant, Font font) {
        PdfPCell cL = new PdfPCell(new Phrase(label, font));
        cL.setPadding(7);
        cL.setBorderColor(COULEUR_BORDURE);
        table.addCell(cL);
        PdfPCell cM = new PdfPCell(new Phrase(montant, font));
        cM.setPadding(7);
        cM.setBorderColor(COULEUR_BORDURE);
        cM.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cM);
    }
}