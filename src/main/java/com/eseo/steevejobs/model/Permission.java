package com.eseo.steevejobs.model;

public class Permission {
    private int id;
    private String codeAction;
    private String description;

    public Permission(int id, String codeAction, String description) {
        this.id = id;
        this.codeAction = codeAction;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeAction() {
        return codeAction;
    }

    public void setCodeAction(String codeAction) {
        this.codeAction = codeAction;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}