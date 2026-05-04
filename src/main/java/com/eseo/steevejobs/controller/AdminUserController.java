package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.ConnexionService;
import com.eseo.steevejobs.service.MailService;
import com.eseo.steevejobs.service.UserService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AdminUserController {

    private final int ROWS_PER_PAGE = 10;
    @FXML
    private Label headerTitle;
    @FXML
    private TextField searchField;
    @FXML
    private Button addUserBtn;
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> colCheckbox;
    @FXML
    private TableColumn<User, String> colName;
    @FXML
    private TableColumn<User, String> colJob;
    @FXML
    private TableColumn<User, String> colRole;
    @FXML
    private TableColumn<User, String> colStatus;
    @FXML
    private TableColumn<User, Void> colActions;
    @FXML
    private Label paginationInfoLabel;
    @FXML
    private Pagination pagination;
    @FXML
    private Label detailsHeaderTitle;
    @FXML
    private Label detailsName;
    @FXML
    private Label detailsJob;
    @FXML
    private Label personalInfoHeader;
    @FXML
    private Label lblEmail;
    @FXML
    private Label detailsEmailVal;
    @FXML
    private Label lblPhone;
    @FXML
    private Label detailsPhoneVal;
    @FXML
    private Label lblAddress;
    @FXML
    private Label detailsAddressVal;
    @FXML
    private Label eventsHeader;
    @FXML
    private VBox eventsContainer;
    private final UserService userService;
    private final ObservableList<User> masterUserList;
    private FilteredList<User> filteredList;

    public AdminUserController() {
        this.userService = new UserService();
        this.masterUserList = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDataFromDatabase();
        setupSearchFilter();

        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        updateUserDetails(newValue);
                    }
                }
        );
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            String prenom = u.getPrenom() != null ? u.getPrenom() : "";
            String nom = u.getNom() != null ? u.getNom() : "";
            return new SimpleStringProperty((prenom + " " + nom).trim());
        });

        colJob.setCellValueFactory(cellData -> {
            String poste = cellData.getValue().getPoste();
            return new SimpleStringProperty(poste != null ? poste : "Non défini");
        });

        colRole.setCellValueFactory(cellData -> {
            String role = cellData.getValue().getRole();
            return new SimpleStringProperty(role != null ? role : "Utilisateur");
        });

        colStatus.setCellValueFactory(cellData -> {
            boolean isActif = cellData.getValue().isActif();
            return new SimpleStringProperty(isActif ? "Actif" : "Inactif");
        });

        setupActionsColumn();
    }

    private void loadDataFromDatabase() {
        try {
            List<User> usersFromDb = userService.getAllUsers();
            masterUserList.setAll(usersFromDb);

            filteredList = new FilteredList<>(masterUserList, b -> true);

            setupPagination();
            updatePaginationInfo();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Base de Données", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(user -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (user.getNom() != null && user.getNom().toLowerCase().contains(lowerCaseFilter)) return true;
                if (user.getPrenom() != null && user.getPrenom().toLowerCase().contains(lowerCaseFilter)) return true;
                if (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseFilter)) return true;
                return user.getPoste() != null && user.getPoste().toLowerCase().contains(lowerCaseFilter);
            });

            setupPagination();
            updatePaginationInfo();
        });
    }

    private void setupPagination() {
        int pageCount = (int) Math.ceil((double) filteredList.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount == 0 ? 1 : pageCount);
        pagination.setPageFactory(this::createPage);
    }

    private Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredList.size());

        SortedList<User> sortedData = new SortedList<>(
                FXCollections.observableArrayList(filteredList.subList(fromIndex, toIndex))
        );
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());

        userTable.setItems(sortedData);

        if (!sortedData.isEmpty()) {
            userTable.getSelectionModel().select(0);
        } else {
            clearUserDetails();
        }

        return new VBox();
    }

    private void updatePaginationInfo() {
        paginationInfoLabel.setText(filteredList.size() + " employés au total");
    }

    private void updateUserDetails(User user) {
        String prenom = user.getPrenom() != null ? user.getPrenom() : "";
        String nom = user.getNom() != null ? user.getNom() : "";
        detailsName.setText((prenom + " " + nom).trim());

        detailsJob.setText(user.getPoste() != null ? user.getPoste() : "Poste non défini");
        detailsEmailVal.setText(user.getEmail() != null ? user.getEmail() : "Non renseigné");
        detailsPhoneVal.setText(user.getTel() != null ? user.getTel() : "Non renseigné");
        detailsAddressVal.setText(user.getAdresse() != null ? user.getAdresse() : "Non renseignée");

        eventsContainer.getChildren().clear();

        if (user.isActif()) {
            eventsContainer.getChildren().add(createEventCard("Aujourd'hui", "Journée de travail", "#5b82d4"));
        } else {
            Label noEvent = new Label("Aucun événement prévu (Compte inactif).");
            noEvent.setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
            eventsContainer.getChildren().add(noEvent);
        }
    }

    private void clearUserDetails() {
        detailsName.setText("");
        detailsJob.setText("");
        detailsEmailVal.setText("");
        detailsPhoneVal.setText("");
        detailsAddressVal.setText("");
        eventsContainer.getChildren().clear();
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                menuButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;");

                MenuItem editItem = new MenuItem("Modifier le profil");
                MenuItem deactivateItem = new MenuItem("Désactiver / Activer");
                MenuItem deleteItem = new MenuItem("Supprimer définitivement");
                deleteItem.setStyle("-fx-text-fill: red;");

                editItem.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    System.out.println("Ouvrir l'édition pour : " + user.getNom());
                });

                deactivateItem.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    try {
                        if (user.isActif()) {
                            userService.deactivateUser(user.getId());
                        } else {
                            userService.activateUser(user.getId());
                        }
                        loadDataFromDatabase();
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    }
                });

                deleteItem.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    try {
                        if (showConfirmation("Confirmer", "Supprimer définitivement " + user.getNom() + " ?")) {
                            userService.deleteUser(user.getId());
                            loadDataFromDatabase();
                        }
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    }
                });

                menuButton.getItems().addAll(editItem, deactivateItem, new SeparatorMenuItem(), deleteItem);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(menuButton);
                }
            }
        });
    }


    @FXML
    private void CreateUser(ActionEvent actionEvent) {
        Label message = new Label("Entrez les informations du nouvel utilisateur :");
        message.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-fill: black; -fx-font-weight: bold;");

        TextField nomField = new TextField();
        nomField.setPromptText("Nom");
        nomField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField prenomField = new TextField();
        prenomField.setPromptText("Prénom");
        prenomField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField telField = new TextField();
        telField.setPromptText("Téléphone");
        telField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Administrateur", "RH", "Employe");
        roleBox.setPromptText("Sélectionnez un rôle");
        roleBox.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");
        roleBox.setMaxWidth(Double.MAX_VALUE);

        TextField posteField = new TextField();
        posteField.setPromptText("Poste (ex: Développeur)");
        posteField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        Label popupMessageLabel = new Label("");
        popupMessageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");

        Button button = new Button("Créer l'utilisateur");
        button.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox buttonBox = new HBox(button);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, message, nomField, prenomField, emailField, telField, roleBox, posteField, popupMessageLabel, buttonBox);
        layout.setStyle("-fx-background-color: #f4f5f7; -fx-padding: 20; -fx-border-color: #d1d5db;");

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Ajouter un Utilisateur");
        popup.setScene(new Scene(layout, 350, 450));
        popup.setResizable(false);
        popup.show();

        button.setOnAction(e -> {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String tel = telField.getText().trim();
            String role = roleBox.getValue();
            String poste = posteField.getText().trim();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || role == null) {
                popupMessageLabel.setText("Veuillez remplir les champs obligatoires (Nom, Prénom, Email, Rôle).");
                popupMessageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            button.setDisable(true);
            popupMessageLabel.setText("Création en cours...");
            popupMessageLabel.setStyle("-fx-text-fill: blue;");

            new Thread(() -> {
                try {
                    if (userService.checkEmailExists(email)) {
                        throw new Exception("Cet email est déjà utilisé par un autre compte.");
                    }

                    String plainToken = ConnexionService.generateRandomMdp(12);
                    String hashedToken = userService.hashPassword(plainToken);

                    User newUser = new User(0, nom, prenom, email, hashedToken, "", role, tel, poste, true);
                    userService.createUser(newUser);

                    MailService.EnvoyerMail(
                            email,
                            "Création de votre compte",
                            "Bonjour " + prenom + ",\n\nVotre compte a été créé avec succès.\n\nVoici votre mot de passe temporaire : " + plainToken + "\n\nPensez à modifier votre mot de passe une fois connecté."
                    );

                    Platform.runLater(() -> {
                        loadDataFromDatabase();
                        popup.close();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "L'utilisateur " + prenom + " " + nom + " a été créé et un email lui a été envoyé.");
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        popupMessageLabel.setText(ex.getMessage() != null ? ex.getMessage() : "Erreur lors de la création.");
                        popupMessageLabel.setStyle("-fx-text-fill: red;");
                        button.setDisable(false);
                    });
                }
            }).start();
        });
    }


    private VBox createEventCard(String time, String title, String color) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        card.setPadding(new Insets(10, 15, 10, 15));

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");

        card.getChildren().addAll(timeLabel, titleLabel);
        return card;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }
}