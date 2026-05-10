package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The type Menu controller.
 */
public class MenuController {

    private static MenuController instance;
    /**
     * The Btn list view.
     */
    @FXML
    public Button btnListView;
    @FXML
    private BorderPane mainPane;
    @FXML
    private Button btnAccueil;
    @FXML
    private Button btnParametres;
    @FXML
    private Button btnPlanning;
    @FXML
    private Button btnTicket;
    @FXML
    private Button btnFiles;

    private Stage mainStage;
    private Label lblTitreHeader;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static MenuController getInstance() {
        return instance;
    }

    /**
     * Initialize.
     */
    @FXML
    public void initialize() {
        instance = this;
        chargerPage("home");
        if (btnAccueil != null) updateButtonStyles(btnAccueil);
    }

    /**
     * Sets composants fenetre.
     *
     * @param stage      the stage
     * @param labelTitre the label titre
     */
    public void setComposantsFenetre(Stage stage, Label labelTitre) {
        this.mainStage = stage;
        this.lblTitreHeader = labelTitre;
    }

    /**
     * Charger page.
     *
     * @param nomFichier the nom fichier
     */
    public void chargerPage(String nomFichier) {
        try {
            String chemin = "/com/eseo/steevejobs/view/" + nomFichier + "-view.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(chemin));
            Parent vue = loader.load();

            if (mainPane != null) {

                mainPane.setCenter(vue);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERREUR CRITIQUE : Impossible de charger " + nomFichier);
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.err.println("ERREUR : Le chemin " + nomFichier + " semble incorrect ou le fichier n'existe pas.");
        }
    }

    /**
     * Afficher accueil.
     *
     * @param event the event
     */
    @FXML
    void afficherAccueil(ActionEvent event) {
        chargerPage("home");
        updateButtonStyles(btnAccueil);
        changerTitre("Accueil");
    }

    /**
     * Afficher emprunt.
     *
     * @param event the event
     */
    @FXML
    void afficherPlanning(ActionEvent event) {
        chargerPage("calendrier");
        updateButtonStyles(btnPlanning);
        changerTitre("Calendrier");
    }

    /**
     * Afficheradherent.
     *
     * @param event the event
     */
    @FXML
    void afficherTicket(ActionEvent event) {
        chargerPage("ticketsList");
        updateButtonStyles(btnTicket);
        changerTitre("ticket");
    }

    /**
     * Afficher add produit.
     *
     * @param event the event
     */
    @FXML
    void afficherFiles(ActionEvent event) {
        chargerPage("ajouter-produit");
        updateButtonStyles(btnFiles);
        changerTitre("Ajouter Produit");
    }

    /**
     * Afficher parametres.
     *
     * @param event the event
     */
    @FXML
    void afficherParametres(ActionEvent event) {
        chargerPage("parametres");
        updateButtonStyles(btnParametres);
        changerTitre("Paramètres");
    }


    private void updateButtonStyles(Button boutonActif) {

        String STYLE_INACTIF = "-fx-cursor: hand; -fx-background-color: transparent;";
        String STYLE_ACTIF = "-fx-cursor: hand; -fx-background-color: transparent;";

        Button[] tousLesBoutons = {btnAccueil, btnPlanning, btnTicket, btnFiles, btnParametres};

        for (Button btn : tousLesBoutons) {
            if (btn != null) {
                btn.setStyle(STYLE_INACTIF);

                SVGPath icone = extraireIcone(btn);
                if (icone != null) {
                    icone.setFill(javafx.scene.paint.Color.WHITE);
                }
            }
        }

        if (boutonActif != null) {
            boutonActif.setStyle(STYLE_ACTIF);
            SVGPath iconeActive = extraireIcone(boutonActif);
            if (iconeActive != null) {
                iconeActive.setFill(javafx.scene.paint.Color.web("#28334D"));
            }
        }
    }

    private SVGPath extraireIcone(Button btn) {
        Object graphic = btn.getGraphic();

        if (graphic instanceof javafx.scene.Group group) {
            if (!group.getChildren().isEmpty() && group.getChildren().get(0) instanceof SVGPath) {
                return (SVGPath) group.getChildren().get(0);
            }
        }
        else if (graphic instanceof SVGPath) {
            return (SVGPath) graphic;
        }

        return null;
    }


    public void changerTitre(String nouveauTitre) {
        HelloApplication.changerTitreGlobal(nouveauTitre);
    }

    public void setCenterView(Parent vue) {
        if (mainPane != null) {
            mainPane.setCenter(vue);
        }
    }
}