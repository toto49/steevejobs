package com.eseo.steevejobs.model.Enum;

public enum AppModule {
    ADMINPANEL("APP_ADMINPANEL_VIEW", "GESTION\nPERMISSION", "Gestion, permissions", "adminpermission", "steevesecu.png", "#7F8C8D"),
    ADMINUSER("APP_ADMINUSER_VIEW", "GESTION\nUTILISATEURS", "Gestion, utilisateur", "adminuser", "steevesecu.png", "#D67C72"),
    STOCKS("APP_STOCKS_VIEW", "GESTION\nDES STOCKS", "Inventaire, alertes", "stock", "steevestock.png", "#76B89A"),
    DOCS("APP_DOCS_VIEW", "GESTION\nCOMMERCIALE", "Devis & Factures", "document", "steevecommercial.png", "#F1CE6C"),
    TICKETS("APP_TICKETS_VIEW", "SUPPORT", "Tickets & Assistance", "ticketsList", "steevesupport.png", "#92B4F2", "services"),
    RH("APP_RH_VIEW", "RESSOURCES\nHUMAINES", "Fiches de Paies", "fiche-paye", "steeverh.png", "#7A9FE0"),
    CALENDRIERRH("APP_RH_VIEW", "CALENDRIER\nRH", "Plannings des employés", "calendrier-rh", "steeverh.png", "#7A9FE0"),
    CLIENTS("APP_RH_VIEW", "GESTION\nCLIENTS", "Création & Gestion des clients", "clients", "steevecommercial.png", "#FF99cc");


    private final String codeAction;
    private final String title;
    private final String subtitle;
    private final String chemin;
    private final String image;
    private final String bgColor;
    private final String filtre;
    AppModule(String codeAction, String title, String subtitle, String chemin, String image, String bgColor) {
        this(codeAction, title, subtitle, chemin, image, bgColor, null);
    }

    AppModule(String codeAction, String title, String subtitle, String chemin, String image, String bgColor, String filtre) {
        this.codeAction = codeAction;
        this.title = title;
        this.subtitle = subtitle;
        this.chemin = chemin;
        this.image = image;
        this.bgColor = bgColor;
        this.filtre = filtre;
    }


    public String getCodeAction() {
        return codeAction;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getChemin() {
        return chemin;
    }

    public String getImage() {
        return image;
    }

    public String getBgColor() {
        return bgColor;
    }

    public String getFiltre() {
        return filtre;
    }
}