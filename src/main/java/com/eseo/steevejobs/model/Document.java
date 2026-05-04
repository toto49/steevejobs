package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Document {
    private int id;
    private DocumentType type;
    private LocalDateTime date;
    private BigDecimal prix_ht;
    private BigDecimal prix_ttc;
    private DocumentStatut statut;
    private String url;
    private Tiers tiers;
    private User creator;

    public Document(int id, DocumentType type, LocalDateTime date,  BigDecimal prix_ht, BigDecimal prix_ttc, DocumentStatut statut, String url) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.prix_ht = prix_ht;
        this.prix_ttc = prix_ttc;
        this.statut = statut;
        this.url = url;
        this.tiers = new Tiers()

    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public DocumentType getType() {
        return type;
    }
    public void setType(DocumentType type) {
        this.type = type;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public BigDecimal getPrix_ht() {
        return prix_ht;
    }
    public void setPrix_ht(BigDecimal prix_ht) {
        this.prix_ht = prix_ht;
    }
    public BigDecimal getPrix_ttc() {
        return prix_ttc;
    }
    public void setPrix_ttc(BigDecimal prix_ttc) {
        this.prix_ttc = prix_ttc;
    }
    public DocumentStatut getStatut() {
        return statut;
    }
    public void setStatut(DocumentStatut statut) {
        this.statut = statut;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

}
