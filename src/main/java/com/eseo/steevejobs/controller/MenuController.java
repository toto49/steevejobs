package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.service.WebSocketService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;

public class MenuController {

    private static MenuController instance;

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

    private Label badgeAccueil;
    private Label badgeTicket;

    public static MenuController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        badgeAccueil = installerBadge(btnAccueil);
        badgeTicket = installerBadge(btnTicket);
        if (btnAccueil != null) updateButtonStyles(btnAccueil);
        chargerPage("home");
        WebSocketService.getInstance().connecter();
    }

    public void setComposantsFenetre(Stage stage, Label labelTitre) {
        this.mainStage = stage;
        this.lblTitreHeader = labelTitre;
    }

    public void chargerPage(String nomFichier) {
        TicketController.fermerChat();
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


    public void allumerBadge(String typeCible, int nombreExact) {
        Platform.runLater(() -> {
            if ("TECH".equals(typeCible)) {
                mettreAJourBadgeRelatif(badgeAccueil, nombreExact);
            } else {
                mettreAJourBadgeRelatif(badgeTicket, nombreExact);
            }
        });
    }

    private void mettreAJourBadgeRelatif(Label badge, int nombre) {
        if (badge == null) return;

        if (nombre <= 0) {
            badge.setVisible(false);
            badge.setText("0");
        } else {
            badge.setText(String.valueOf(nombre));
            badge.setVisible(true);
        }
    }

    private void incrementerBadge(Label badge) {
        if (badge == null) return;

        if (!badge.isVisible()) {
            badge.setText("1");
            badge.setVisible(true);
        } else {
            int count = Integer.parseInt(badge.getText());
            badge.setText(String.valueOf(count + 1));
        }
    }

    private Label installerBadge(Button bouton) {
        if (bouton == null || bouton.getGraphic() == null) return null;

        Label badge = new Label("1");
        badge.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 1 5 1 5; -fx-font-size: 10px; -fx-font-weight: bold;");
        badge.setVisible(false);
        badge.setMouseTransparent(true);

        Node iconeActuelle = bouton.getGraphic();

        StackPane calque = new StackPane();
        calque.getChildren().addAll(iconeActuelle, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(0, 14, 0, 0));

        bouton.setGraphic(calque);

        return badge;
    }


    @FXML
    void afficherAccueil(ActionEvent event) {
        chargerPage("home");
        updateButtonStyles(btnAccueil);
        changerTitre("Accueil");
    }


    public void effacerBadgeAccueil() {
        if (badgeAccueil != null) {
            badgeAccueil.setVisible(false);
            badgeAccueil.setText("0");
        }
    }

    @FXML
    void afficherPlanning(ActionEvent event) {
        chargerPage("calendrier");
        updateButtonStyles(btnPlanning);
        changerTitre("Calendrier");
    }
    @FXML
    void afficherTicket(ActionEvent event) {
        TicketController.fermerChat();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/ticketsList-view.fxml"));
            Parent root = loader.load();

            TicketsListController controller = loader.getController();
            controller.afficherMesTickets();

            setCenterView(root);
            updateButtonStyles(btnTicket);
            changerTitre("Tickets");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de l'ouverture de la vue ticketsList");
        }
    }

    public void effacerBadgeticket() {
        if (badgeTicket != null) {
            badgeTicket.setVisible(false);
            badgeTicket.setText("0");
        }
    }

    @FXML
    void afficherFiles(ActionEvent event) {
        chargerPage("document");
        updateButtonStyles(btnFiles);
        changerTitre("Document");
    }

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
        if (graphic instanceof StackPane stack) {
            for (Node n : stack.getChildren()) {
                if (n instanceof SVGPath) return (SVGPath) n;
                if (n instanceof javafx.scene.Group group && !group.getChildren().isEmpty() && group.getChildren().get(0) instanceof SVGPath) {
                    return (SVGPath) group.getChildren().get(0);
                }
            }
        } else if (graphic instanceof javafx.scene.Group group) {
            if (!group.getChildren().isEmpty() && group.getChildren().get(0) instanceof SVGPath) {
                return (SVGPath) group.getChildren().get(0);
            }
        } else if (graphic instanceof SVGPath) {
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