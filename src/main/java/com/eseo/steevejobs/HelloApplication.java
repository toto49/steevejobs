package com.eseo.steevejobs;

import atlantafx.base.theme.PrimerLight;
import com.eseo.steevejobs.service.WebSocketService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

/**
 * Application JavaFX principale SteeveJobs.
 * Gère la fenêtre sans bordure (header personnalisé), la navigation globale
 * via {@link #changerPageGlobale} et le redimensionnement par les bords.
 */
public class HelloApplication extends Application {

    private static Label lblTitreHeader;
    private static Stage mainStage;
    private static VBox rootGlobal;
    private static HBox headerGlobal;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isMaximized = false;
    private double savedX, savedY, savedWidth, savedHeight;

    /**
     * Point d'entrée JavaFX de l'application.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Met à jour le titre affiché dans le header et la barre système.
     *
     * @param nouveauTitre suffixe du titre (préfixé par « SteeveJobs - »)
     */
    public static void changerTitreGlobal(String nouveauTitre) {
        String titreComplet = "SteeveJobs - " + nouveauTitre;
        if (lblTitreHeader != null) {
            lblTitreHeader.setText(titreComplet);
        }
        if (mainStage != null) {
            mainStage.setTitle(titreComplet);
        }
    }

    /**
     * Remplace la vue centrale et met à jour le titre global.
     * Utilisé par la navigation post-connexion et la déconnexion.
     *
     * @param nouvelleVue contenu à afficher sous le header
     * @param nouveauTitre titre associé à la page
     */
    public static void changerPageGlobale(Parent nouvelleVue, String nouveauTitre) {
        VBox.setVgrow(nouvelleVue, Priority.ALWAYS);
        if (rootGlobal != null && rootGlobal.getChildren().size() > 1) {
            rootGlobal.getChildren().set(1, nouvelleVue);
        }
        changerTitreGlobal(nouveauTitre);
    }

    /**
     * Initialise la scène principale : thème, header, vue de bienvenue, icône et affichage.
     *
     * @param stage fenêtre principale
     * @throws IOException si le chargement FXML {@code bienvenue-view.fxml} échoue
     */
    @Override
    public void start(Stage stage) throws IOException {
        mainStage = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.initStyle(StageStyle.TRANSPARENT);
        headerGlobal = creerHeaderPersonnalise(stage);

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("view/bienvenue-view.fxml"));
        Parent vuePrincipale = fxmlLoader.load();

        VBox.setVgrow(vuePrincipale, Priority.ALWAYS);
        rootGlobal = new VBox(headerGlobal, vuePrincipale);

        Scene scene = new Scene(rootGlobal);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
        stage.getIcons().add(icon);
        stage.setScene(scene);

        stage.setMinWidth(600);
        stage.setMinHeight(400);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        savedWidth = 1024;
        savedHeight = 768;
        savedX = (bounds.getWidth() - savedWidth) / 2;
        savedY = (bounds.getHeight() - savedHeight) / 2;

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        isMaximized = true;


        appliquerArrondis(false);

        ResizeHelper.addResizeListener(stage);

        changerTitreGlobal("Bienvenue");
        stage.show();
    }

    private void appliquerArrondis(boolean arrondir) {
        if (arrondir) {
            rootGlobal.setStyle("-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #555555; -fx-border-width: 1; -fx-background-color: -color-bg-default;");
            if (headerGlobal != null) {
                headerGlobal.setStyle("-fx-background-color: #4b78cc; -fx-padding: 0 0 0 8; -fx-background-radius: 11 11 0 0;");
            }
            Rectangle masque = new Rectangle();
            masque.widthProperty().bind(rootGlobal.widthProperty());
            masque.heightProperty().bind(rootGlobal.heightProperty());
            masque.setArcWidth(24);
            masque.setArcHeight(24);

            rootGlobal.setClip(masque);

        } else {
            rootGlobal.setStyle("-fx-background-radius: 0; -fx-border-radius: 0; -fx-border-width: 0; -fx-background-color: -color-bg-default;");
            if (headerGlobal != null) {
                headerGlobal.setStyle("-fx-background-color: #4b78cc; -fx-padding: 0 0 0 8; -fx-background-radius: 0;");
            }
            rootGlobal.setClip(null);
        }
    }

    private void saveWindowBounds(Stage stage) {
        javafx.collections.ObservableList<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen currentScreen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);

        if (stage.getHeight() < currentScreen.getVisualBounds().getHeight()) {
            savedX = stage.getX();
            savedY = stage.getY();
            savedWidth = stage.getWidth();
            savedHeight = stage.getHeight();
        }
    }

    private void toggleMaximize(Stage stage) {
        if (isMaximized) {
            stage.setX(savedX);
            stage.setY(savedY);
            stage.setWidth(savedWidth);
            stage.setHeight(savedHeight);
            isMaximized = false;
            appliquerArrondis(true);
        } else {
            saveWindowBounds(stage);
            javafx.collections.ObservableList<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen currentScreen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
            Rectangle2D bounds = currentScreen.getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            isMaximized = true;
            appliquerArrondis(false);
        }
    }

    private HBox creerHeaderPersonnalise(Stage stage) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        String svgReduire = "M 0,5 H 10 V 6 H 0 Z";
        Button btnReduire = creerBoutonHeader(svgReduire, "#F2F2F2", "#000000", false);
        btnReduire.setOnAction(e -> stage.setIconified(true));

        String svgAgrandir = "M 0,0 H 10 V 10 H 0 Z M 1,1 V 9 H 9 V 1 Z";
        Button btnAgrandir = creerBoutonHeader(svgAgrandir, "#F2F2F2", "#000000", false);
        btnAgrandir.setOnAction(e -> toggleMaximize(stage));

        String svgFermer = "M 1,0 L 5,4 L 9,0 L 10,1 L 6,5 L 10,9 L 9,10 L 5,6 L 1,10 L 0,9 L 4,5 L 0,1 Z";
        Button btnFermer = creerBoutonHeader(svgFermer, "#e81123", "#ffffff", true);
        btnFermer.setOnAction(e -> {
            WebSocketService.getInstance().deconnecter(() -> {
                Platform.exit();
            });
        });


        header.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        header.setOnMouseDragged(event -> {
            if (isMaximized) {
                isMaximized = false;
                stage.setWidth(savedWidth);
                stage.setHeight(savedHeight);
                xOffset = savedWidth / 2;
            }
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
            appliquerArrondis(true);
        });

        header.setOnMouseReleased(event -> {
            javafx.collections.ObservableList<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen currentScreen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
            Rectangle2D bounds = currentScreen.getVisualBounds();

            double mouseX = event.getScreenX();
            double mouseY = event.getScreenY();
            // Snap vers le haut : maximisation plein écran de l'écran courant
            if (mouseY <= bounds.getMinY() + 5) {
                if (!isMaximized) {
                    saveWindowBounds(stage);
                    stage.setX(bounds.getMinX());
                    stage.setY(bounds.getMinY());
                    stage.setWidth(bounds.getWidth());
                    stage.setHeight(bounds.getHeight());
                    isMaximized = true;
                    appliquerArrondis(false);
                }
            // Snap vers la gauche : moitié gauche de l'écran
            } else if (mouseX <= bounds.getMinX() + 5) {
                saveWindowBounds(stage);
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth() / 2);
                stage.setHeight(bounds.getHeight());
                isMaximized = false;
                appliquerArrondis(false);
            // Snap vers la droite : moitié droite de l'écran
            } else if (mouseX >= bounds.getMaxX() - 5) {
                saveWindowBounds(stage);
                stage.setX(bounds.getMinX() + (bounds.getWidth() / 2));
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth() / 2);
                stage.setHeight(bounds.getHeight());
                isMaximized = false;
                appliquerArrondis(false);
            }
        });

        header.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize(stage);
            }
        });

        header.getChildren().addAll(spacer, btnReduire, btnAgrandir, btnFermer);
        return header;
    }

    private Button creerBoutonHeader(String svgData, String hoverBgColor, String hoverIconColor, boolean estBoutonCoin) {
        Button btn = new Button();
        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(svgData);
        icon.setFill(javafx.scene.paint.Color.web("#000000"));
        btn.setGraphic(icon);

        btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 15; -fx-background-radius: 0; -fx-border-radius: 0;");

        btn.setOnMouseEntered(e -> {
            String radius = (estBoutonCoin && !isMaximized) ? "0 11 0 0" : "0";

            btn.setStyle("-fx-background-color: " + hoverBgColor + "; -fx-padding: 3 15; -fx-background-radius: " + radius + "; -fx-border-radius: 0; -fx-cursor: hand;");
            icon.setFill(javafx.scene.paint.Color.web(hoverIconColor));
        });

        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 3 15; -fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: default;");
            icon.setFill(javafx.scene.paint.Color.web("#000000"));
        });

        return btn;
    }


    /**
     * Utilitaire de redimensionnement par les bords de la fenêtre sans bordure.
     */
    public static class ResizeHelper {

        /**
         * Attache les gestionnaires de souris permettant le redimensionnement sur les bords de la scène.
         *
         * @param stage fenêtre à redimensionner
         */
        public static void addResizeListener(Stage stage) {
            ResizeListener resizeListener = new ResizeListener(stage);
            stage.getScene().addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, resizeListener);
            stage.getScene().addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, resizeListener);
            stage.getScene().addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, resizeListener);
        }

        /**
         * Écouteur de redimensionnement par les bords de la scène.
         */
        private static class ResizeListener implements javafx.event.EventHandler<javafx.scene.input.MouseEvent> {
            private final Stage stage;
            private final int border = 5;
            private javafx.scene.Cursor cursorEvent = javafx.scene.Cursor.DEFAULT;
            private double startX = 0, startY = 0;

            /**
             * Crée l'écouteur de redimensionnement pour la fenêtre donnée.
             *
             * @param stage fenêtre cible
             */
            public ResizeListener(Stage stage) {
                this.stage = stage;
            }

            /**
             * Gère le curseur, la capture et le redimensionnement selon la position du pointeur.
             *
             * @param mouseEvent événement souris (MOUSE_MOVED, MOUSE_PRESSED ou MOUSE_DRAGGED)
             */
            @Override
            public void handle(javafx.scene.input.MouseEvent mouseEvent) {
                javafx.event.EventType<? extends javafx.scene.input.MouseEvent> mouseEventType = mouseEvent.getEventType();
                Scene scene = stage.getScene();
                double mouseEventX = mouseEvent.getSceneX(), mouseEventY = mouseEvent.getSceneY(), sceneWidth = scene.getWidth(), sceneHeight = scene.getHeight();

                if (javafx.scene.input.MouseEvent.MOUSE_MOVED.equals(mouseEventType)) {
                    if (mouseEventX < border && mouseEventY < border) cursorEvent = javafx.scene.Cursor.NW_RESIZE;
                    else if (mouseEventX < border && mouseEventY > sceneHeight - border)
                        cursorEvent = javafx.scene.Cursor.SW_RESIZE;
                    else if (mouseEventX > sceneWidth - border && mouseEventY < border)
                        cursorEvent = javafx.scene.Cursor.NE_RESIZE;
                    else if (mouseEventX > sceneWidth - border && mouseEventY > sceneHeight - border)
                        cursorEvent = javafx.scene.Cursor.SE_RESIZE;
                    else if (mouseEventX < border) cursorEvent = javafx.scene.Cursor.W_RESIZE;
                    else if (mouseEventX > sceneWidth - border) cursorEvent = javafx.scene.Cursor.E_RESIZE;
                    else if (mouseEventY < border) cursorEvent = javafx.scene.Cursor.N_RESIZE;
                    else if (mouseEventY > sceneHeight - border) cursorEvent = javafx.scene.Cursor.S_RESIZE;
                    else cursorEvent = javafx.scene.Cursor.DEFAULT;
                    scene.setCursor(cursorEvent);
                } else if (javafx.scene.input.MouseEvent.MOUSE_PRESSED.equals(mouseEventType)) {
                    startX = stage.getWidth() - mouseEventX;
                    startY = stage.getHeight() - mouseEventY;
                } else if (javafx.scene.input.MouseEvent.MOUSE_DRAGGED.equals(mouseEventType)) {
                    if (!javafx.scene.Cursor.DEFAULT.equals(cursorEvent)) {
                        if (!javafx.scene.Cursor.W_RESIZE.equals(cursorEvent) && !javafx.scene.Cursor.E_RESIZE.equals(cursorEvent)) {
                            double minHeight = stage.getMinHeight() > (border * 2) ? stage.getMinHeight() : (border * 2);
                            if (javafx.scene.Cursor.NW_RESIZE.equals(cursorEvent) || javafx.scene.Cursor.N_RESIZE.equals(cursorEvent) || javafx.scene.Cursor.NE_RESIZE.equals(cursorEvent)) {
                                if (stage.getHeight() > minHeight || mouseEventY < 0) {
                                    stage.setHeight(stage.getY() - mouseEvent.getScreenY() + stage.getHeight());
                                    stage.setY(mouseEvent.getScreenY());
                                }
                            } else {
                                if (stage.getHeight() > minHeight || mouseEventY + startY - stage.getHeight() > 0)
                                    stage.setHeight(mouseEventY + startY);
                            }
                        }
                        if (!javafx.scene.Cursor.N_RESIZE.equals(cursorEvent) && !javafx.scene.Cursor.S_RESIZE.equals(cursorEvent)) {
                            double minWidth = stage.getMinWidth() > (border * 2) ? stage.getMinWidth() : (border * 2);
                            if (javafx.scene.Cursor.NW_RESIZE.equals(cursorEvent) || javafx.scene.Cursor.W_RESIZE.equals(cursorEvent) || javafx.scene.Cursor.SW_RESIZE.equals(cursorEvent)) {
                                if (stage.getWidth() > minWidth || mouseEventX < 0) {
                                    stage.setWidth(stage.getX() - mouseEvent.getScreenX() + stage.getWidth());
                                    stage.setX(mouseEvent.getScreenX());
                                }
                            } else {
                                if (stage.getWidth() > minWidth || mouseEventX + startX - stage.getWidth() > 0)
                                    stage.setWidth(mouseEventX + startX);
                            }
                        }
                    }
                }
            }
        }
    }
}