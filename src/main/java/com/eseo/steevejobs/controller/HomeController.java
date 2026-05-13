package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.HelloApplication;
import com.eseo.steevejobs.model.Enum.AppModule;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class HomeController {
    private UserService userService;
    private SessionService sessionService;
    private User currentUser;

    private final PermissionService permissionService;
    @FXML
    private FlowPane appsGrid;
    private List<String> currentUserPermissions;
    public static int notificationsTech = 0;
    public static int notificationsAuteur = 0;

    private static HomeController activeInstance;

    private Label badgeCarteTech;
    private Label badgeCarteAuteur;

    public static HomeController getActiveInstance() {
        return activeInstance;
    }

    public HomeController() {
        this.permissionService = new PermissionService();
    }

    public static void ajouterNotification(String typeCible) {
        System.out.println("🔔 [HOME] Signal reçu pour le type : " + typeCible);

        Platform.runLater(() -> {
            if ("TECH".equals(typeCible)) {
                notificationsTech++;
                System.out.println("🔔 [HOME] Nombre de notifs TECH en mémoire : " + notificationsTech);

                if (activeInstance == null) {
                    System.out.println("⚠️ [HOME] La page d'accueil n'est pas ouverte (activeInstance est null). La mémoire est bien stockée pour plus tard.");
                } else if (activeInstance.badgeCarteTech == null) {
                    System.err.println("❌ [HOME] ERREUR : La page est ouverte mais badgeCarteTech est NULL ! Le codeAction 'APP_TICKETS_VIEW' n'existe sûrement pas dans ton Enum AppModule.");
                } else {
                    activeInstance.badgeCarteTech.setText(String.valueOf(notificationsTech));
                    activeInstance.badgeCarteTech.setVisible(true);
                    System.out.println("✅ [HOME] Le badge de la carte s'est allumé avec succès !");
                }
            }
        });
    }

    public void onUserLogin(int idUserConnecte) {
        this.currentUserPermissions = permissionService.getUserPermissions(idUserConnecte);

        TicketService ticketService = new TicketServiceImpl();

        if ("ADMIN".equals(currentUser.getRole()) || "RH".equals(currentUser.getRole())) {
            notificationsTech = ticketService.getNombreTicketsNonLusAdmin(currentUser.getRole());
            if (MenuController.getInstance() != null && notificationsTech > 0) {
                MenuController.getInstance().allumerBadge("TECH");
            }
        } else {
            notificationsAuteur = ticketService.getNombreTicketsNonLusAuteur(currentUser.getId());
            if (MenuController.getInstance() != null && notificationsAuteur > 0) {
                MenuController.getInstance().allumerBadge("AUTEUR");
            }
        }

        renderAppCenter();
    }

    @FXML
    public void initialize() throws IOException {
        activeInstance = this;
        this.currentUser = SessionService.getUtilisateurConnecte();

        if (this.currentUser != null) {
            onUserLogin(currentUser.getId());
        } else {
            SessionService.setUtilisateurConnecte(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/bienvenue-view.fxml"));
            Parent loginRoot = loader.load();

            HelloApplication.changerPageGlobale(loginRoot, "Connexion");
        }
    }

    private void renderAppCenter() {
        appsGrid.getChildren().clear();

        for (AppModule app : AppModule.values()) {
            if (hasPermission(app.getCodeAction())) {

                String parametre = null;
                String codeAction = app.getCodeAction();
                if ("APP_TICKETS_VIEW".equals(codeAction)) {
                    parametre = currentUser.getRole();
                }


                Label badgeDynamique = new Label();
                badgeDynamique.setVisible(false);


                if ("APP_TICKETS_VIEW".equals(codeAction)) {
                    badgeCarteTech = badgeDynamique;
                    if (notificationsTech > 0) {
                        badgeDynamique.setText(String.valueOf(notificationsTech));
                        badgeDynamique.setVisible(true);
                    }
                } else if ("MES_TICKETS".equals(codeAction)) {
                    badgeCarteAuteur = badgeDynamique;
                    if (notificationsAuteur > 0) {
                        badgeDynamique.setText(String.valueOf(notificationsAuteur));
                        badgeDynamique.setVisible(true);
                    }
                }

                HBox card = createAppCard(
                        app.getTitle(),
                        app.getSubtitle(),
                        badgeDynamique,
                        app.getBgColor(),
                        app.getChemin(),
                        app.getImage(),
                        parametre,
                        codeAction
                );

                card.prefWidthProperty().bind(appsGrid.widthProperty().divide(3).subtract(60));
                card.prefHeightProperty().bind(card.widthProperty().multiply(0.6));

                appsGrid.getChildren().add(card);
            }
        }
    }

    private void chargerPageAvecParametre(String chemin, String parametre, String titreCard) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/eseo/steevejobs/view/" + chemin + "-view.fxml"));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ParametrizedController) {
                ((ParametrizedController) controller).initData(parametre);
            }
            MenuController.getInstance().setCenterView(view);
            MenuController.getInstance().changerTitre(titreCard);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Erreur lors de l'ouverture de la vue paramétrée : " + chemin);
        }
    }

    private HBox createAppCard(String title, String subtitle, Label badge, String bgColor, String chemin, String image, String parametreFacultatif, String codeAction) {
        HBox card = new HBox();
        card.setMinSize(250, 220);
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 15;");
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setAlignment(Pos.CENTER_LEFT);

        VBox imagePlaceholder = new VBox();
        imagePlaceholder.setStyle("-fx-alignment: center;");
        imagePlaceholder.prefWidthProperty().bind(card.widthProperty().multiply(0.3));

        ImageView imageView = new ImageView(
                new Image(getClass().getResource("/images/" + image).toExternalForm())
        );
        imageView.setPreserveRatio(true);
        imageView.fitWidthProperty().bind(card.widthProperty().multiply(0.28));
        imageView.fitHeightProperty().bind(imageView.fitWidthProperty());

        imagePlaceholder.getChildren().add(imageView);

        VBox textZone = new VBox(5);
        textZone.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", card.widthProperty().divide(14), "px; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold;")
        );

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.styleProperty().bind(
                Bindings.concat("-fx-font-size: ", card.widthProperty().divide(22), "px; -fx-text-fill: #333333;")
        );

        textZone.getChildren().addAll(lblTitle, lblSubtitle);

        if (badge != null) {
            badge.styleProperty().bind(
                    Bindings.concat("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 15 5 15; -fx-font-size: ", card.widthProperty().divide(26), "px;")
            );
            VBox.setMargin(badge, new Insets(10, 0, 0, 0));
            textZone.getChildren().add(badge);
        }

        card.getChildren().addAll(imagePlaceholder, textZone);
        card.setOnMouseEntered(e -> card.setOpacity(0.8));
        card.setOnMouseExited(e -> card.setOpacity(1.0));
        card.setOnMouseClicked(e -> {

            if (MenuController.getInstance() != null) {
                if (parametreFacultatif != null) {
                    chargerPageAvecParametre(chemin, parametreFacultatif, title);
                } else {
                    MenuController.getInstance().chargerPage(chemin);
                    MenuController.getInstance().changerTitre(title);
                }
            } else {
                System.err.println("Erreur : MenuController n'est pas initialisé.");
            }
        });

        return card;
    }

    private boolean hasPermission(String requiredPermission) {
        return currentUserPermissions != null && currentUserPermissions.contains(requiredPermission);
    }
}