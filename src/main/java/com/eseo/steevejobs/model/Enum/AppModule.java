package com.eseo.steevejobs.model.Enum;

/**
 * Catalogue des modules fonctionnels de l'application (tuile d'accueil).
 * Chaque constante lie un code permission, des libellés UI, une route Vaadin et des assets visuels ; non persistée en base.
 */
public enum AppModule {
    /** Administration des permissions. */
    ADMINPANEL("APP_ADMINPANEL_VIEW", "GESTION\nPERMISSION", "Gestion, permissions", "adminpermission", "steevesecu.png", "#8A9BB5"),
    /** Gestion des comptes utilisateurs. */
    ADMINUSER("APP_ADMINUSER_VIEW", "GESTION\nUTILISATEURS", "Gestion, utilisateur", "adminuser", "steevesecu.png", "#F08682"),
    /** Inventaire et alertes stock. */
    STOCKS("APP_STOCKS_VIEW", "GESTION\nDES STOCKS", "Inventaire, alertes", "stock", "steevestock.png", "#5BBA96"),
    /** Devis, factures et documents commerciaux. */
    DOCS("APP_DOCS_VIEW", "GESTION\nCOMMERCIALE", "Devis & Factures", "document", "steevecommercial.png", "#F3C86A"),
    /** Support et tickets. */
    TICKETS("APP_TICKETS_VIEW", "SUPPORT", "Tickets & Assistance", "ticketsList", "steevesupport.png", "#7BC6F0"),
    /** Fiches de paie et dossier RH employé. */
    RH("APP_RH_VIEW", "RESSOURCES\nHUMAINES", "Fiches de Paies", "fiche-paye", "steeverh.png", "#A88BDD"),
    /** Calendrier et plannings RH. */
    CALENDRIERRH("APP_CALENDRIER_RH_VIEW", "CALENDRIER\nRH", "Plannings des employés", "calendrier-rh", "steeverh.png", "#A5AEFF"),
    /** Référentiel clients (tiers). */
    CLIENTS("APP_RH_CLIENT_VIEW", "GESTION\nCLIENTS", "Création & Gestion des clients", "clients", "steevecommercial.png", "#F59A76");

    private final String codeAction;
    private final String title;
    private final String subtitle;
    private final String chemin;
    private final String image;
    private final String bgColor;
    private final String filtre;

    /**
     * @param codeAction code permission requis pour afficher le module
     * @param title      titre principal de la tuile
     * @param subtitle   sous-titre descriptif
     * @param chemin     segment de route Vaadin
     * @param image      nom de fichier icône
     * @param bgColor    couleur de fond (hex)
     */
    AppModule(String codeAction, String title, String subtitle, String chemin, String image, String bgColor) {
        this(codeAction, title, subtitle, chemin, image, bgColor, null);
    }

    /**
     * @param codeAction code permission
     * @param title      titre tuile
     * @param subtitle   sous-titre
     * @param chemin     route
     * @param image      icône
     * @param bgColor    couleur de fond
     * @param filtre     filtre optionnel (ex. type tiers en liste clients)
     */
    AppModule(String codeAction, String title, String subtitle, String chemin, String image, String bgColor, String filtre) {
        this.codeAction = codeAction;
        this.title = title;
        this.subtitle = subtitle;
        this.chemin = chemin;
        this.image = image;
        this.bgColor = bgColor;
        this.filtre = filtre;
    }

    /** @return code d'action de permission */
    public String getCodeAction() {
        return codeAction;
    }

    /** @return titre affiché sur la tuile */
    public String getTitle() {
        return title;
    }

    /** @return sous-titre de la tuile */
    public String getSubtitle() {
        return subtitle;
    }

    /** @return chemin de navigation UI */
    public String getChemin() {
        return chemin;
    }

    /** @return nom de l'image associée */
    public String getImage() {
        return image;
    }

    /** @return couleur de fond hexadécimale */
    public String getBgColor() {
        return bgColor;
    }

    /** @return filtre métier optionnel, ou {@code null} */
    public String getFiltre() {
        return filtre;
    }
}
