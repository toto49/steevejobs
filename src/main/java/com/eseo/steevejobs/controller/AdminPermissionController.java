package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.Permission;
import com.eseo.steevejobs.service.PermissionService;
import com.eseo.steevejobs.util.TestRuntime;
import javafx.animation.FillTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

public class AdminPermissionController {

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private VBox permissionsContainer;

    private PermissionService permissionService;

    private List<Permission> toutesLesPermissionsCache;

    @FXML
    public void initialize() {
        this.permissionService = new PermissionService();
        roleComboBox.getItems().addAll("ADMIN", "RH", "EMPLOYE");
        if (TestRuntime.isEnabled()) {
            return;
        }
        permissionService.syncAppModulePermissions();
        this.toutesLesPermissionsCache = permissionService.getAllPermissions();

        roleComboBox.setOnAction(event -> {
            String roleChoisi = roleComboBox.getValue();
            if (roleChoisi != null) {
                chargerPermissionsPourRole(roleChoisi);
            }
        });

        roleComboBox.getSelectionModel().select("ADMIN");
        chargerPermissionsPourRole("ADMIN");
    }

    public void chargerPermissionsPourRole(String nomRole) {
        permissionsContainer.getChildren().clear();
        Label loadingLabel = new Label("Chargement des permissions...");
        loadingLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        permissionsContainer.getChildren().add(loadingLabel);

        new Thread(() -> {
            List<Integer> permissionsActives = permissionService.getPermissionIdsByRole(nomRole);
            Platform.runLater(() -> {
                permissionsContainer.getChildren().clear();

                List<HBox> listeDeBoutons = new java.util.ArrayList<>();
                for (Permission perm : toutesLesPermissionsCache) {
                    boolean isActive = permissionsActives.contains(perm.getId());
                    HBox customSwitch = createCustomSwitch(perm, isActive, nomRole);
                    listeDeBoutons.add(customSwitch);
                }
                permissionsContainer.getChildren().addAll(listeDeBoutons);
            });

        }).start();
    }

    private HBox createCustomSwitch(Permission perm, boolean isSelected, String nomRole) {
        HBox container = new HBox(15);
        container.setAlignment(Pos.CENTER_LEFT);

        Label permLabel = new Label(perm.getDescription() + " (" + perm.getCodeAction() + ")");
        permLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

        StackPane switchPane = new StackPane();
        switchPane.setPrefSize(110, 35);
        switchPane.setMinSize(110, 35);
        switchPane.setCursor(javafx.scene.Cursor.HAND);

        Rectangle background = new Rectangle(110, 35);
        background.setArcWidth(35);
        background.setArcHeight(35);
        Label textYes = new Label("ACTIVÉ");
        textYes.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        textYes.setTranslateX(-15);

        Label textNo = new Label("DÉSACTIVÉ");
        textNo.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;");
        textNo.setTranslateX(15);

        Circle thumb = new Circle(14);
        thumb.setFill(Color.WHITE);

        switchPane.getChildren().addAll(background, textYes, textNo, thumb);
        double positionGauche = -35;
        double positionDroite = 35;
        if (isSelected) {
            background.setFill(Color.web("#2ECC71"));
            thumb.setTranslateX(positionDroite);
            textYes.setOpacity(1);
            textNo.setOpacity(0);
        } else {
            background.setFill(Color.web("#E74C3C"));
            thumb.setTranslateX(positionGauche);
            textYes.setOpacity(0);
            textNo.setOpacity(1);
        }

        final boolean[] currentState = {isSelected};

        switchPane.setOnMouseClicked(e -> {
            boolean willBeSelected = !currentState[0];
            currentState[0] = willBeSelected;
            TranslateTransition transition = new TranslateTransition(Duration.millis(150), thumb);
            transition.setToX(willBeSelected ? positionDroite : positionGauche);
            transition.play();
            FillTransition fillTransition = new FillTransition(Duration.millis(150), background);
            fillTransition.setToValue(willBeSelected ? Color.web("#2ECC71") : Color.web("#E74C3C"));
            fillTransition.play();
            textYes.setOpacity(willBeSelected ? 1 : 0);
            textNo.setOpacity(willBeSelected ? 0 : 1);
            new Thread(() -> {
                if (willBeSelected) {
                    permissionService.assignPermissionToRole(nomRole, perm.getId());
                } else {
                    permissionService.revokePermissionFromRole(nomRole, perm.getId());
                }
            }).start();
        });

        container.getChildren().addAll(switchPane, permLabel);
        return container;
    }

    private void updateSwitchVisuals(boolean isSelected, Rectangle bg, Circle thumb, Label yes, Label no) {
        if (isSelected) {
            bg.setFill(Color.web("#2ECC71"));
            thumb.setFill(Color.WHITE);
            StackPane.setAlignment(thumb, Pos.CENTER_RIGHT);
            StackPane.setMargin(thumb, new Insets(0, 3, 0, 0));
            yes.setVisible(true);
            no.setVisible(false);
        } else {
            bg.setFill(Color.web("#E74C3C"));
            thumb.setFill(Color.WHITE);
            StackPane.setAlignment(thumb, Pos.CENTER_LEFT);
            StackPane.setMargin(thumb, new Insets(0, 0, 0, 3));
            yes.setVisible(false);
            no.setVisible(true);
        }
    }
}