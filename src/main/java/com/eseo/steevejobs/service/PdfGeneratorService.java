package com.eseo.steevejobs.service;

import com.eseo.steevejobs.controller.HomeController;
import com.eseo.steevejobs.model.Composer;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.Image;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.io.File;

public class PdfGeneratorService {
 // A CHANGER -************************************************************************************
    private static final String OUTPUT_DIR = "documents/";

    private static final Color COULEUR_PRINCIPALE = new Color(75, 120, 204);
    private static final Color COULEUR_GRIS       = new Color(107, 114, 128);
    private static final Color COULEUR_FOND_LIGNE = new Color(244, 245, 247);
    private static final Color COULEUR_BORDURE    = new Color(209, 213, 219);

    // -------------------------------------------------------
    // PDF GENERER PAR OPEN PDF
    // -------------------------------------------------------

    // A CHANGER *************************************************************************************
    public String genererDocument(com.eseo.steevejobs.model.Document document, List<Composer> lignes) {
        creerDossier();
        String nomFichier = String.format("%s_%d.pdf",
                document.getType().getValeur().replace(" ", "_"),
                document.getId());
        String chemin = OUTPUT_DIR + nomFichier;

        System.out.println("=== GÉNÉRATION PDF ===");
        System.out.println("Type: " + document.getType().getValeur());
        System.out.println("Nom fichier: " + nomFichier);
        System.out.println("Chemin complet: " + new File(chemin).getAbsolutePath());

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

    public String genererFichePaye(FichePaye fiche, double salaireBase,
                                   double tauxCotisations, long joursConge) {
        creerDossier();
        String nomFichier = String.format("fiche_%d_%d_%02d.pdf",
                fiche.getEmploye().getId(),
                fiche.getMois().getYear(),
                fiche.getMois().getMonthValue());
        String chemin = OUTPUT_DIR + nomFichier;

        try (FileOutputStream fos = new FileOutputStream(chemin)) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(PageSize.A4, 55, 55, 60, 60);
            PdfWriter.getInstance(doc, fos);
            doc.open();
            ajouterContenuFichePaye(doc, fiche, salaireBase, tauxCotisations, joursConge);
            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération PDF fiche de paie : " + e.getMessage(), e);
        }
        return chemin;
    }
    private Image chargerLogo() {
        try {
            // Charger l'image depuis les ressources
            InputStream is = getClass().getResourceAsStream("/images/logo.png");
            if (is == null) {
                System.err.println("Logo non trouvé : /images/logo.png");
                return null;
            }
            Image logo = Image.getInstance(is.readAllBytes());

            // Redimensionner le logo (largeur 80px, hauteur automatique)
            logo.scaleToFit(80, 80);
            return logo;
        } catch (Exception e) {
            System.err.println("Erreur chargement logo : " + e.getMessage());
            return null;
        }
    }
    // -------------------------------------------------------
    // CONTENU — DOCUMENT : devis, facture, bon de commande
    // -------------------------------------------------------

    private void ajouterContenuDocument(com.lowagie.text.Document doc,
                                        com.eseo.steevejobs.model.Document document,
                                        List<Composer> lignes) throws DocumentException {
        // logo steeve costume en haut a droite
        Image logo = chargerLogo();
        if (logo != null) {
            logo.setAlignment(Element.ALIGN_RIGHT);
            logo.setSpacingAfter(5);
            doc.add(logo);
        }
        //style
        Font fontTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COULEUR_PRINCIPALE);
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COULEUR_PRINCIPALE);
        Font fontNormal  = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        Font fontBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font fontGris    = FontFactory.getFont(FontFactory.HELVETICA, 10, COULEUR_GRIS);
        Font fontTotal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COULEUR_PRINCIPALE);

        String typeLabel = switch (document.getType()) {
            case DEVIS           -> "DEVIS";
            case FACTURE         -> "FACTURE";
            case BON_COMMANDE    -> "BON DE COMMANDE";
        };

        Paragraph titre = new Paragraph(typeLabel, fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingBefore(0);
        titre.setSpacingAfter(2);
        doc.add(titre);

        Paragraph ref = new Paragraph("N° " + document.getId() + "  —  " +
                document.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        ref.setAlignment(Element.ALIGN_CENTER);
        doc.add(ref);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // SECTION CLIENT
        doc.add(new Paragraph("CLIENT", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableInfos = new PdfPTable(2);
        tableInfos.setWidthPercentage(100);
        tableInfos.setWidths(new float[]{1, 3});
        tableInfos.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        tableInfos.getDefaultCell().setPadding(2);

        ajouterInfoLigne(tableInfos, "Nom",     document.getTiers().getNom(),     fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Email",   document.getTiers().getEmail(),   fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Adresse", document.getTiers().getAdresse(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Tél",     document.getTiers().getTel(),     fontGris, fontNormal);
        if (document.getTiers().getSiret() != null) {
            ajouterInfoLigne(tableInfos, "SIRET", document.getTiers().getSiret(), fontGris, fontNormal);
        }
        doc.add(tableInfos);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // SECTION VENDEUR
        doc.add(new Paragraph("VENDEUR", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableVendeur = new PdfPTable(2);
        tableVendeur.setWidthPercentage(100);
        tableVendeur.setWidths(new float[]{1, 3});
        tableVendeur.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        tableVendeur.getDefaultCell().setPadding(2);

        User vendeur = document.getEditeur();
        if (vendeur != null) {
            String nomComplet = (vendeur.getPrenom() != null ? vendeur.getPrenom() : "") + " " + (vendeur.getNom() != null ? vendeur.getNom() : "");
            ajouterInfoLigne(tableVendeur, "Nom", nomComplet.trim().isEmpty() ? "Non renseigné" : nomComplet, fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Poste", vendeur.getPoste() != null ? vendeur.getPoste() : "Non renseigné", fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Email", vendeur.getEmail() != null ? vendeur.getEmail() : "Non renseigné", fontGris, fontNormal);
        } else {
            ajouterInfoLigne(tableVendeur, "Nom", "Non renseigné", fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Poste", "Non renseigné", fontGris, fontNormal);
            ajouterInfoLigne(tableVendeur, "Email", "Non renseigné", fontGris, fontNormal);
        }
        doc.add(tableVendeur);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        // SECTION DÉTAIL DES PRESTATIONS
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
        ajouterLigneTotaux(tableTotaux, "Total HT",  String.format("%.2f €", document.getPrixHt()),  fontNormal);
        ajouterLigneTotaux(tableTotaux, "Total TTC", String.format("%.2f €", document.getPrixTtc()), fontTotal);
        doc.add(tableTotaux);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);

        // STATUT
        Paragraph statut = new Paragraph("Statut : " + document.getStatut().name(), fontGris);
        statut.setAlignment(Element.ALIGN_RIGHT);
        doc.add(statut);

        doc.add(new Paragraph(" "));

        // FOOTER
        Paragraph footer = new Paragraph(
                "Document généré par SteevéJobs — " +
                        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // -------------------------------------------------------
    // CONTENU — FICHE DE PAIE EN LIEN AVEC PLANNING
    // -------------------------------------------------------

    private void ajouterContenuFichePaye(com.lowagie.text.Document doc, FichePaye fiche,
                                         double salaireBase, double tauxCotisations,
                                         long joursConge) throws DocumentException {
        //ajoute le steeve coustume en haut a droite
        Image logo = chargerLogo();
        if (logo != null) {
            logo.setAlignment(Element.ALIGN_RIGHT);
            logo.setSpacingAfter(10);
            doc.add(logo);
        }
        Font fontTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COULEUR_PRINCIPALE);
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COULEUR_PRINCIPALE);
        Font fontNormal  = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        Font fontBold    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font fontGris    = FontFactory.getFont(FontFactory.HELVETICA, 10, COULEUR_GRIS);
        Font fontNet     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COULEUR_PRINCIPALE);

        String periode = fiche.getMois()
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

        doc.add(new Paragraph("INFORMATIONS EMPLOYÉ", fontSection));
        doc.add(new Paragraph(" "));

        PdfPTable tableInfos = new PdfPTable(2);
        tableInfos.setWidthPercentage(100);
        tableInfos.setWidths(new float[]{1, 2});
        tableInfos.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        tableInfos.getDefaultCell().setPadding(4);

        ajouterInfoLigne(tableInfos, "Nom",   fiche.getEmploye().getPrenom() + " " + fiche.getEmploye().getNom(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Poste", fiche.getEmploye().getPoste(), fontGris, fontNormal);
        ajouterInfoLigne(tableInfos, "Email", fiche.getEmploye().getEmail(), fontGris, fontNormal);
        doc.add(tableInfos);

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        doc.add(new Paragraph(" "));

        doc.add(new Paragraph("DÉTAIL DE LA RÉMUNÉRATION", fontSection));
        doc.add(new Paragraph(" "));

        double tauxJournalier    = salaireBase / 22.0;
        double deductionConges   = joursConge > 0 ? tauxJournalier * joursConge : 0;
        double salaireBrutAjuste = salaireBase - deductionConges;
        double cotisations       = salaireBrutAjuste * tauxCotisations;
        double netAPayer         = salaireBrutAjuste - cotisations;

        PdfPTable tableMontants = new PdfPTable(3);
        tableMontants.setWidthPercentage(100);
        tableMontants.setWidths(new float[]{5, 2, 2});

        for (String entete : new String[]{"Libellé", "Taux / Détail", "Montant"}) {
            PdfPCell cell = new PdfPCell(new Phrase(entete, fontBold));
            cell.setBackgroundColor(COULEUR_FOND_LIGNE);
            cell.setPadding(7);
            cell.setBorderColor(COULEUR_BORDURE);
            tableMontants.addCell(cell);
        }

        ajouterLigneMontant(tableMontants, "Salaire de base mensuel", "",
                String.format("%.2f €", salaireBase), fontNormal, false);

        if (joursConge > 0) {
            ajouterLigneMontant(tableMontants,
                    "Congés (" + joursConge + " jour(s) détecté(s))",
                    String.format("%.2f € / jour", tauxJournalier),
                    String.format("- %.2f €", deductionConges),
                    fontNormal, true);
        }

        ajouterLigneMontant(tableMontants, "Salaire brut après congés", "",
                String.format("%.2f €", salaireBrutAjuste), fontNormal, false);

        ajouterLigneMontant(tableMontants, "Cotisations salariales",
                String.format("%.1f %%", tauxCotisations * 100),
                String.format("- %.2f €", cotisations), fontNormal, true);

        PdfPCell cLib = new PdfPCell(new Phrase("NET À PAYER", fontNet));
        cLib.setBorder(com.lowagie.text.Rectangle.TOP); cLib.setPadding(8);
        tableMontants.addCell(cLib);
        PdfPCell cDet = new PdfPCell(new Phrase("", fontNet));
        cDet.setBorder(com.lowagie.text.Rectangle.TOP); cDet.setPadding(8);
        tableMontants.addCell(cDet);
        PdfPCell cNet = new PdfPCell(new Phrase(String.format("%.2f €", netAPayer), fontNet));
        cNet.setBorder(com.lowagie.text.Rectangle.TOP);
        cNet.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cNet.setPadding(8);
        tableMontants.addCell(cNet);

        doc.add(tableMontants);

        if (joursConge > 0) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(
                    "ⓘ  " + joursConge + " jour(s) de congé détecté(s) automatiquement depuis le planning.",
                    fontGris));
        }

        doc.add(new Paragraph(" "));
        ajouterSeparateur(doc);
        Paragraph footer = new Paragraph(
                "Document généré automatiquement par SteevéJobs " +
                        java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontGris);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    private void creerDossier() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier : " + OUTPUT_DIR, e);
        }
    }

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

    private void ajouterInfoLigne(PdfPTable table, String label, String valeur,
                                  Font fLabel, Font fValeur) {
        PdfPCell cL = new PdfPCell(new Phrase(label, fLabel));
        cL.setBorder(com.lowagie.text.Rectangle.NO_BORDER); cL.setPadding(4);
        table.addCell(cL);
        PdfPCell cV = new PdfPCell(new Phrase(valeur != null ? valeur : "", fValeur));
        cV.setBorder(com.lowagie.text.Rectangle.NO_BORDER); cV.setPadding(4);
        table.addCell(cV);
    }

    private void ajouterCelluleLigne(PdfPTable table, String texte, Font font,
                                     Color fond, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(fond);
        cell.setPadding(7);
        cell.setBorderColor(COULEUR_BORDURE);
        cell.setHorizontalAlignment(alignement);
        table.addCell(cell);
    }

    private void ajouterLigneMontant(PdfPTable table, String libelle, String detail,
                                     String montant, Font font, boolean alterne) {
        Color fond = alterne ? COULEUR_FOND_LIGNE : Color.WHITE;
        PdfPCell cL = new PdfPCell(new Phrase(libelle, font));
        cL.setBackgroundColor(fond); cL.setPadding(7); cL.setBorderColor(COULEUR_BORDURE);
        table.addCell(cL);
        PdfPCell cD = new PdfPCell(new Phrase(detail, font));
        cD.setBackgroundColor(fond); cD.setPadding(7); cD.setBorderColor(COULEUR_BORDURE);
        table.addCell(cD);
        PdfPCell cM = new PdfPCell(new Phrase(montant, font));
        cM.setBackgroundColor(fond); cM.setPadding(7); cM.setBorderColor(COULEUR_BORDURE);
        cM.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cM);
    }

    private void ajouterLigneTotaux(PdfPTable table, String label,
                                    String montant, Font font) {
        PdfPCell cL = new PdfPCell(new Phrase(label, font));
        cL.setPadding(7); cL.setBorderColor(COULEUR_BORDURE);
        table.addCell(cL);
        PdfPCell cM = new PdfPCell(new Phrase(montant, font));
        cM.setPadding(7); cM.setBorderColor(COULEUR_BORDURE);
        cM.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cM);
    }
}