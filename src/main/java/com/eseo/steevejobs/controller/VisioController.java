package com.eseo.steevejobs.controller;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class VisioController {

    @FXML
    private WebView webView;

    @FXML
    public void initialize() {
        WebEngine webEngine = webView.getEngine();

        // Autoriser le JavaScript et les accès multimédia
        webEngine.setJavaScriptEnabled(true);

        // Chargement du fichier HTML
        String url = getClass().getResource("/com/eseo/steevejobs/view/visio.html").toExternalForm();
        webEngine.load(url);
    }
}