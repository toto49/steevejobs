package com.eseo.steevejobs.model;

/**
 * Représente une permission applicative associée à un code d'action.
 * Persistée en base pour le contrôle d'accès aux modules ; exposée dans l'administration des droits.
 */
public class Permission {
    private int id;
    private String codeAction;
    private String description;

    /**
     * Construit une permission avec ses attributs métier.
     *
     * @param id          identifiant technique
     * @param codeAction  code d'action (ex. vue module)
     * @param description libellé descriptif
     */
    public Permission(int id, String codeAction, String description) {
        this.id = id;
        this.codeAction = codeAction;
        this.description = description;
    }

    /** @return identifiant technique */
    public int getId() {
        return id;
    }

    /** @param id identifiant technique */
    public void setId(int id) {
        this.id = id;
    }

    /** @return code d'action associé à la permission */
    public String getCodeAction() {
        return codeAction;
    }

    /** @param codeAction code d'action associé à la permission */
    public void setCodeAction(String codeAction) {
        this.codeAction = codeAction;
    }

    /** @return description lisible de la permission */
    public String getDescription() {
        return description;
    }

    /** @param description description lisible de la permission */
    public void setDescription(String description) {
        this.description = description;
    }
}
