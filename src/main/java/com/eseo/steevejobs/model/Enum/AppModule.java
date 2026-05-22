package com.eseo.steevejobs.model.Enum;

public enum AppModule {
    ADMINPANEL("APP_ADMINPANEL_VIEW", "GESTION\nPERMISSION", "Gestion, permissions", "adminpermission", "happy.png", "#D67C72"),
    ADMINUSER("APP_ADMINUSER_VIEW", "GESTION\nUTILISATEURS", "Gestion, utilisateur", "adminuser", "happy.png", "#D67C72"),
    STOCKS("APP_STOCKS_VIEW", "GESTION\nDES STOCKS", "Inventaire, alertes", "adminuser", "steevestock.png", "#76B89A"),
    DOCS("APP_DOCS_VIEW", "GESTION\nCOMMERCIALE", "Devis & Factures", "test", "happy.png", "#F1CE6C"),
    TICKETS("APP_TICKETS_VIEW", "SUPPORT", "Tickets & Assistance", "ticketsList", "happy.png", "#92B4F2", "services"), // <-- Chemin mis à jour
    RH("APP_RH_VIEW", "RESSOURCES\nHUMAINES", "Plannings & Paies", "test", "happy.png", "#FF99cc");

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