package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.util.TestRuntime;
import com.eseo.steevejobs.service.SystemNotificationService;
import com.eseo.steevejobs.service.WebSocketService;
import com.eseo.steevejobs.service.WebSocketUiBridge;
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

/**
 * Contrôleur FXML du menu latéral et du conteneur central ({@code menu-view.fxml}).
 * Liaisons FXML : {@code mainPane}, boutons de navigation ({@code btnAccueil}, {@code btnPlanning}, etc.).
 * Gère le chargement des vues, les badges de notification et les callbacks WebSocket.
 */
public class MenuController {

    /** Singleton du contrôleur menu pour accès depuis les autres vues. */
    private static MenuController instance;

    /** Bouton basculant l'affichage en mode liste. */
    @FXML
    public Button btnListView;
    /** Conteneur principal de la mise en page. */
    @FXML
    private BorderPane mainPane;
    /** Bouton de navigation vers l'accueil. */
    @FXML
    private Button btnAccueil;
    /** Bouton de navigation vers les paramètres. */
    @FXML
    private Button btnParametres;
    /** Bouton de navigation vers le calendrier. */
    @FXML
    private Button btnPlanning;
    /** Bouton de navigation vers les tickets. */
    @FXML
    private Button btnTicket;
    /** Bouton de navigation vers les documents. */
    @FXML
    private Button btnFiles;
    /** Bouton de navigation vers la visioconférence. */
    @FXML
    private Button btnVisio;

    /** Fenêtre principale de l'application. */
    private Stage mainStage;
    /** Label affichant le titre global du header. */
    private Label lblTitreHeader;

    /** Badge de notification sur le bouton accueil. */
    private Label badgeAccueil;
    /** Badge de notification sur le bouton tickets. */
    private Label badgeTicket;

    /**
     * Retourne l'instance singleton du contrôleur menu.
     *
     * @return instance courante ou {@code null} avant initialisation FXML
     */
    public static MenuController getInstance() {
        return instance;
    }

    /**
     * Enregistre l'instance, installe les badges et charge la page d'accueil.
     * En mode test, n'ouvre pas la connexion WebSocket ni la page home.
     */
    @FXML
    public void initialize() {
        instance = this;
        badgeAccueil = installerBadge(btnAccueil);
        badgeTicket = installerBadge(btnTicket);

        if (btnAccueil != null) updateButtonStyles(btnAccueil);
        enregistrerCallbacksWebSocket();
        if (TestRuntime.isEnabled()) {
            return;
        }
        chargerPage("home");
        WebSocketService.getInstance().connecter();
    }

    /**
     * Enregistre les callbacks WebSocket pour les notifications tickets.
     */
    private void enregistrerCallbacksWebSocket() {
        WebSocketUiBridge.getInstance().setTicketCallbacks(new WebSocketUiBridge.TicketCallbacks() {
            /**
             * Rafraîchit le chat si le ticket concerné est ouvert.
             *
             * @param ticketId identifiant du ticket notifié
             * @return {@code true} si le chat actif a été rafraîchi
             */
            @Override
            public boolean tryRefreshChatIfActive(int ticketId) {
                TicketController chatActif = TicketController.getActiveInstance();
                if (chatActif != null && chatActif.getCurrentTicketId() == ticketId) {
                    chatActif.refreshChatSilently();
                    return true;
                }
                return false;
            }

            /**
             * Demande le rafraîchissement de la liste tickets si elle est affichée.
             */
            @Override
            public void onRefreshTicketList() {
                TicketsListController listeActive = TicketsListController.getActiveInstance();
                if (listeActive != null) {
                    listeActive.rafraichirAffichage();
                }
            }

            /**
             * Met à jour badges et notifications push selon le type de destinataire.
             *
             * @param targetType {@code TECH} ou {@code AUTEUR}
             * @param pushEnabled {@code true} si les notifications système sont activées
             */
            @Override
            public void onTicketNotification(String targetType, boolean pushEnabled) {
                HomeController.ajouterNotification(targetType);
                if (pushEnabled) {
                    if ("AUTEUR".equals(targetType)) {
                        SystemNotificationService.send("SteeveJobs - Support", "Nouvelle réponse reçue");
                    } else if ("TECH".equals(targetType)) {
                        SystemNotificationService.send("SteeveJobs - Admin", "Nouveau message à traiter !");
                    }
                }
            }
        });
    }

    /**
     * Lie le contrôleur menu à la fenêtre principale et au label de titre global.
     *
     * @param stage fenêtre principale
     * @param labelTitre label de titre du header (peut être {@code null})
     */
    public void setComposantsFenetre(Stage stage, Label labelTitre) {
        this.mainStage = stage;
        this.lblTitreHeader = labelTitre;
    }

    /**
     * Charge une vue FXML dans la zone centrale et ferme les sessions chat/visio actives.
     *
     * @param nomFichier nom de base du FXML (sans suffixe {@code -view.fxml})
     */
    public void chargerPage(String nomFichier) {
        TicketController.fermerChat();

        if (VisioController.getActiveInstance() != null) {
            VisioController.getActiveInstance().couperController();
        }

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
     * Met à jour le badge de notification sur le bouton menu correspondant.
     *
     * @param typeCible {@code TECH} (accueil) ou {@code AUTEUR} (tickets)
     * @param nombreExact nombre à afficher ; masqué si inférieur ou égal à 0
     */
    public void allumerBadge(String typeCible, int nombreExact) {
        Platform.runLater(() -> mettreAJourBadge(typeCible, nombreExact));
    }

    /**
     * Met à jour le badge correspondant au type de destinataire.
     *
     * @param typeCible {@code TECH} ou {@code AUTEUR}
     * @param nombre nombre à afficher sur le badge
     */
    private void mettreAJourBadge(String typeCible, int nombre) {
        Label badge;
        if ("TECH".equals(typeCible)) {
            if (badgeAccueil == null) {
                badgeAccueil = installerBadge(btnAccueil);
            }
            badge = badgeAccueil;
        } else {
            if (badgeTicket == null) {
                badgeTicket = installerBadge(btnTicket);
            }
            badge = badgeTicket;
        }

        if (badge == null) {
            return;
        }

        if (nombre <= 0) {
            badge.setVisible(false);
            badge.setText("0");
        } else {
            badge.setText(String.valueOf(nombre));
            badge.setVisible(true);
        }
    }

    /**
     * Incrémente le compteur affiché sur un badge de notification.
     *
     * @param badge label badge à mettre à jour
     */
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

    /**
     * Installe ou récupère le badge de notification superposé à l'icône d'un bouton menu.
     *
     * @param bouton bouton de navigation concerné
     * @return label badge ou {@code null} si le bouton est invalide
     */
    private Label installerBadge(Button bouton) {
        if (bouton == null) {
            return null;
        }
        if (bouton.getGraphic() == null) {
            return null;
        }

        if (bouton.getGraphic() instanceof StackPane stack) {
            for (Node child : stack.getChildren()) {
                if (child instanceof Label label) {
                    return label;
                }
            }
        }

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

    /**
     * Affiche la page d'accueil.
     * Liaison FXML : {@code btnAccueil}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void afficherAccueil(ActionEvent event) {
        chargerPage("home");
        updateButtonStyles(btnAccueil);
        changerTitre("Accueil");
    }

    /**
     * Réinitialise le badge de notification de l'accueil.
     */
    public void effacerBadgeAccueil() {
        if (badgeAccueil != null) {
            badgeAccueil.setVisible(false);
            badgeAccueil.setText("0");
        }
    }

    /**
     * Affiche la page calendrier.
     * Liaison FXML : {@code btnPlanning}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void afficherPlanning(ActionEvent event) {
        chargerPage("calendrier");
        updateButtonStyles(btnPlanning);
        changerTitre("Calendrier");
    }

    /**
     * Affiche la liste des tickets de l'utilisateur connecté.
     * Liaison FXML : {@code btnTicket}.
     *
     * @param event événement du bouton (non utilisé)
     */
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

    /**
     * Réinitialise le badge de notification des tickets.
     */
    public void effacerBadgeticket() {
        if (badgeTicket != null) {
            badgeTicket.setVisible(false);
            badgeTicket.setText("0");
        }
    }

    /**
     * Affiche la page documents utilisateur.
     * Liaison FXML : {@code btnFiles}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void afficherFiles(ActionEvent event) {
        chargerPage("documentUser");
        updateButtonStyles(btnFiles);
        changerTitre("Document");
    }

    /**
     * Affiche la page visioconférence.
     * Liaison FXML : {@code btnVisio}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void afficherVisio(ActionEvent event) {
        chargerPage("visio");
        updateButtonStyles(btnVisio);
        changerTitre("Visioconférence");
    }

    /**
     * Affiche la page paramètres.
     * Liaison FXML : {@code btnParametres}.
     *
     * @param event événement du bouton (non utilisé)
     */
    @FXML
    void afficherParametres(ActionEvent event) {
        chargerPage("parametres");
        updateButtonStyles(btnParametres);
        changerTitre("Paramètres");
    }

    /**
     * Met en surbrillance le bouton de navigation actif et réinitialise les autres.
     *
     * @param boutonActif bouton correspondant à la page affichée
     */
    private void updateButtonStyles(Button boutonActif) {
        String STYLE_INACTIF = "-fx-cursor: hand; -fx-background-color: transparent;";
        String STYLE_ACTIF = "-fx-cursor: hand; -fx-background-color: transparent;";

        Button[] tousLesBoutons = {btnAccueil, btnPlanning, btnTicket, btnFiles, btnParametres, btnVisio};

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

    /**
     * Extrait le chemin SVG affiché comme graphique d'un bouton menu.
     *
     * @param btn bouton dont l'icône est recherchée
     * @return icône SVG ou {@code null} si introuvable
     */
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

    /**
     * Met à jour le titre global de la fenêtre via {@link HelloApplication}.
     *
     * @param nouveauTitre titre affiché
     */
    public void changerTitre(String nouveauTitre) {
        HelloApplication.changerTitreGlobal(nouveauTitre);
    }

    /**
     * Remplace directement le contenu central du {@code BorderPane} principal.
     *
     * @param vue racine de la vue à afficher
     */
    public void setCenterView(Parent vue) {
        if (mainPane != null) {
            mainPane.setCenter(vue);
        }
    }


}