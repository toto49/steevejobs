package com.eseo.steevejobs.model;

import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.model.Enum.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Document {
    private int id;
    private DocumentType type;
    private LocalDateTime date;
    private BigDecimal prixHt;
    private BigDecimal prixTtc;
    private DocumentStatut statut;
    private String url;
    private Tiers tiers;
    private final User creator;

    public Document(int id, DocumentType type, LocalDateTime date, BigDecimal prixHt, BigDecimal prixTtc, DocumentStatut statut, String url, Tiers tiers, User creator) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.prixHt = prixHt;
        this.prixTtc = prixTtc;
        this.statut = statut;
        this.url = url;
        this.tiers = tiers;
        this.creator = creator;

    }

    public Tiers getTiers() {
        return tiers;
    }

    public void setTiers(Tiers tiers) {
        this.tiers = tiers;
    }

    public User getCreator() {
        return creator;
    }

    public User SetCreator(User creator) {
        return this.creator;
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

    public BigDecimal getPrixHt() {
        return prixHt;
    }

    public void setPrixHt(BigDecimal prixHt) {
        this.prixHt = prixHt;
    }

    public BigDecimal getPrixTtc() {
        return prixTtc;
    }

    public void setPrixTtc(BigDecimal prixTtc) {
        this.prixTtc = prixTtc;
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