package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.TiersType;

import java.util.ArrayList;
import java.util.List;

public class Tiers {
    private int id;
    private String nom;
    private String prenom;
    private TiersType type;
    private String email;
    private String adresse;
    private String tel;
    private String siret;
    private String num_tva;
    private boolean actif;
    private List<Document> documents;

    public Tiers() { this.documents = new ArrayList<>();}

    public Tiers( int id, String nom, String prenom, TiersType type, String email, String adresse, String tel, String siret, String num_tva ) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.type = type;
        this.email = email;
        this.adresse = adresse;
        this.tel = tel;
        this.siret = siret;
        this.num_tva = num_tva;
        this.actif = true;

        this.documents = new ArrayList<>();
    }

    public int  getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }
    public String getPrenom() {
        return prenom;
    }
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }
    public TiersType getType() {
        return type;
    }
    public void setType(TiersType type) {
        this.type = type;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }
    public String getTel() {
        return tel;
    }
    public void setTel(String tel) {
        this.tel = tel;
    }
    public String getSiret() {
        return siret;
    }
    public void setSiret(String siret) {
        this.siret = siret;
    }
    public String getNum_tva() {
        return num_tva;
    }
    public void setNum_tva(String num_tva) {
        this.num_tva = num_tva;
    }
    public boolean isActif() {
        return actif;
    }
    public void setActif(boolean actif) {
        this.actif = actif;
    }
    public List<Document> getDocuments() {
        return documents;
    }
    public void addDocument(Document document) {
        this.documents.add(document);
    }
    public void removeDocument(Document document) {
        this.documents.remove(document);
    }
}
