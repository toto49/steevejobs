package com.eseo.steevejobs.model;

public class VisioInvitations {
    private int id;
    private int visio_id;
    private int employe_id;


    public VisioInvitations() {
    }

    public VisioInvitations(int id, int visio_id, int employe_id) {
        this.id = id;
        this.visio_id = visio_id;
        this.employe_id = employe_id;
    }

    public VisioInvitations(int visio_id, int employe_id) {
        this.visio_id = visio_id;
        this.employe_id = employe_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVisio_id() {
        return visio_id;
    }

    public void setVisio_id(int visio_id) {
        this.visio_id = visio_id;
    }

    public int getEmploye_id() {
        return employe_id;
    }

    public void setEmploye_id(int employe_id) {
        this.employe_id = employe_id;
    }

}

