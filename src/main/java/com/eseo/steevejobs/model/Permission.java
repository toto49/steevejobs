package com.eseo.steevejobs.model;

public class Permission {
    private int id;
    private String code_action;
    private String description;
    // utile de faire une liste de tous les utilisateurs qui ont un certain role ?

    public Permission(int id, String code_action, String description) {
        this.id = id;
        this.code_action = code_action;
        this.description = description;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getCode_action() {
        return code_action;
    }
    public void setCode_action(String code_action) {
        this.code_action = code_action;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}

