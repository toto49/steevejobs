package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.ConnexionService;
import com.eseo.steevejobs.service.MailService;
import com.eseo.steevejobs.service.UserService;
import com.eseo.steevejobs.util.TestRuntime;
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

/**
 * Contrôleur FXML de gestion des utilisateurs (liste paginée, recherche, détail, création et édition).
 * Liaisons FXML : {@code userTable}, {@code searchField}, {@code addUserBtn}, panneau de détail.
 */
public class AdminUserController {

    /** Nombre de lignes affichées par page dans le tableau. */
    private final int ROWS_PER_PAGE = 10;
    /** Titre de l'en-tête de la page de gestion des utilisateurs. */
    @FXML
    private Label headerTitle;
    /** Champ de recherche textuelle sur les utilisateurs. */
    @FXML
    private TextField searchField;
    /** Bouton d'ouverture du formulaire de création d'utilisateur. */
    @FXML
    private Button addUserBtn;
    /** Tableau paginé des utilisateurs. */
    @FXML
    private TableView<User> userTable;
    /** Colonne de cases à cocher du tableau. */
    @FXML
    private TableColumn<User, String> colCheckbox;
    /** Colonne affichant le nom complet de l'utilisateur. */
    @FXML
    private TableColumn<User, String> colName;
    /** Colonne affichant le poste de l'utilisateur. */
    @FXML
    private TableColumn<User, String> colJob;
    /** Colonne affichant le rôle de l'utilisateur. */
    @FXML
    private TableColumn<User, String> colRole;
    /** Colonne affichant le statut actif ou inactif. */
    @FXML
    private TableColumn<User, String> colStatus;
    /** Colonne des actions (modifier, activer/désactiver). */
    @FXML
    private TableColumn<User, Void> colActions;
    /** Libellé indiquant le nombre total d'employés filtrés. */
    @FXML
    private Label paginationInfoLabel;
    /** Contrôle de pagination du tableau. */
    @FXML
    private Pagination pagination;
    /** Titre de l'en-tête du panneau de détail. */
    @FXML
    private Label detailsHeaderTitle;
    /** Nom affiché dans le panneau de détail. */
    @FXML
    private Label detailsName;
    /** Poste affiché dans le panneau de détail. */
    @FXML
    private Label detailsJob;
    /** Titre de la section informations personnelles. */
    @FXML
    private Label personalInfoHeader;
    /** Libellé du champ e-mail dans le détail. */
    @FXML
    private Label lblEmail;
    /** Valeur de l'e-mail dans le panneau de détail. */
    @FXML
    private Label detailsEmailVal;
    /** Libellé du champ téléphone dans le détail. */
    @FXML
    private Label lblPhone;
    /** Valeur du téléphone dans le panneau de détail. */
    @FXML
    private Label detailsPhoneVal;
    /** Libellé du champ adresse dans le détail. */
    @FXML
    private Label lblAddress;
    /** Valeur de l'adresse dans le panneau de détail. */
    @FXML
    private Label detailsAddressVal;
    /** Titre de la section événements du panneau de détail. */
    @FXML
    private Label eventsHeader;
    /** Conteneur des cartes d'événements associées à l'utilisateur. */
    @FXML
    private VBox eventsContainer;
    /** Service d'accès et de persistance des utilisateurs. */
    private final UserService userService;
    /** Liste observable complète des utilisateurs chargés. */
    private final ObservableList<User> masterUserList;
    /** Liste filtrée selon la recherche, source de la pagination. */
    private FilteredList<User> filteredList;

    /**
     * Initialise les services et la liste observable des utilisateurs.
     */
    public AdminUserController() {
        this.userService = new UserService();
        this.masterUserList = FXCollections.observableArrayList();
    }

    /**
     * Configure les colonnes du tableau, charge les utilisateurs et branche la recherche.
     * En mode test, initialise uniquement la pagination sans accès base de données.
     *
     * @throws SQLException non propagée ; affichée via une alerte en cas d'échec de chargement
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        if (TestRuntime.isEnabled()) {
            filteredList = new FilteredList<>(masterUserList, b -> true);
            setupPagination();
            return;
        }
        loadDataFromDatabase();
        setupSearchFilter();

        userTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (observable.getValue() != null) {
                        updateUserDetails(observable.getValue());
                    }
                }
        );
    }

    /**
     * Configure les fabriques de valeurs et la colonne d'actions du tableau utilisateurs.
     */
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

    /**
     * Charge tous les utilisateurs depuis la base et initialise la liste filtrée et la pagination.
     */
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

    /**
     * Branche le champ de recherche sur le prédicat de filtrage et la pagination.
     */
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

    /**
     * Recalcule le nombre de pages et associe la fabrique de pages à la pagination.
     */
    private void setupPagination() {
        int pageCount = (int) Math.ceil((double) filteredList.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount == 0 ? 1 : pageCount);
        pagination.setPageFactory(this::createPage);
    }

    /**
     * Construit le contenu d'une page du tableau paginé.
     *
     * @param pageIndex index de la page (0-based)
     * @return nœud racine vide ; les lignes sont portées par {@code userTable}
     */
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

    /**
     * Met à jour le libellé indiquant le nombre total d'employés filtrés.
     */
    private void updatePaginationInfo() {
        paginationInfoLabel.setText(filteredList.size() + " employés au total");
    }

    /**
     * Affiche les informations détaillées de l'utilisateur sélectionné dans le panneau latéral.
     *
     * @param user utilisateur dont le détail doit être affiché
     */
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

    /**
     * Réinitialise le panneau de détail lorsqu'aucun utilisateur n'est sélectionné.
     */
    private void clearUserDetails() {
        detailsName.setText("");
        detailsJob.setText("");
        detailsEmailVal.setText("");
        detailsPhoneVal.setText("");
        detailsAddressVal.setText("");
        eventsContainer.getChildren().clear();
    }

    /**
     * Configure la colonne d'actions avec menu modifier et activer/désactiver.
     */
    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final MenuButton menuButton = new MenuButton("⋮");

            {
                menuButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;");

                MenuItem editItem = new MenuItem("Modifier le profil");
                MenuItem deactivateItem = new MenuItem("Désactiver / Activer");
                deactivateItem.setStyle("-fx-text-fill: red;");

                editItem.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    showEditUserPopup(user);
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

                menuButton.getItems().addAll(editItem, new SeparatorMenuItem(), deactivateItem);
            }

            /**
             * Affiche le menu d'actions ou une cellule vide.
             *
             * @param item non utilisé (colonne sans valeur)
             * @param empty {@code true} si la ligne est hors plage
             */
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

    /**
     * Ouvre une fenêtre modale pour modifier le profil ou réinitialiser le mot de passe.
     *
     * @param user utilisateur à éditer
     */
    private void showEditUserPopup(User user) {
        Label message = new Label("Modifier les informations de l'utilisateur :");
        message.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-fill: black; -fx-font-weight: bold;");

        TextField nomField = new TextField(user.getNom() != null ? user.getNom() : "");
        nomField.setPromptText("Nom");
        nomField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField prenomField = new TextField(user.getPrenom() != null ? user.getPrenom() : "");
        prenomField.setPromptText("Prénom");
        prenomField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField emailField = new TextField(user.getEmail() != null ? user.getEmail() : "");
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField adresseField = new TextField(user.getAdresse() != null ? user.getAdresse() : "");
        adresseField.setPromptText("Adresse");
        adresseField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField telField = new TextField(user.getTel() != null ? user.getTel() : "");
        telField.setPromptText("Téléphone");
        telField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "RH", "Employe");
        roleBox.setValue(user.getRole());
        roleBox.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");
        roleBox.setMaxWidth(Double.MAX_VALUE);

        TextField posteField = new TextField(user.getPoste() != null ? user.getPoste() : "");
        posteField.setPromptText("Poste (ex: Développeur)");
        posteField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        Label popupMessageLabel = new Label("");
        popupMessageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: red;");

        Button btnSave = new Button("Enregistrer");
        btnSave.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button btnResetPwd = new Button("Réinit. Mot de passe");
        btnResetPwd.setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        HBox buttonBox = new HBox(15, btnResetPwd, btnSave);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(10, message, nomField, prenomField, emailField, adresseField, telField, roleBox, posteField, popupMessageLabel, buttonBox);
        layout.setStyle("-fx-background-color: #f4f5f7; -fx-padding: 20; -fx-border-color: #d1d5db;");

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Modifier l'Utilisateur");
        popup.setScene(new Scene(layout, 400, 550));
        popup.setResizable(false);
        popup.show();

        btnSave.setOnAction(e -> {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String adresse = adresseField.getText().trim();
            String tel = telField.getText().trim();
            String role = roleBox.getValue();
            String poste = posteField.getText().trim();

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || role == null) {
                popupMessageLabel.setText("Veuillez remplir les champs obligatoires (Nom, Prénom, Email, Rôle).");
                popupMessageLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            try {
                user.setNom(nom);
                user.setPrenom(prenom);
                user.setEmail(email);
                user.setAdresse(adresse);
                user.setTel(tel);
                user.setRole(role);
                user.setPoste(poste);

                userService.updateUser(user);

                loadDataFromDatabase();
                popup.close();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Le profil a été mis à jour avec succès.");
            } catch (Exception ex) {
                popupMessageLabel.setText("Erreur lors de la mise à jour : " + ex.getMessage());
                popupMessageLabel.setStyle("-fx-text-fill: red;");
            }
        });

        btnResetPwd.setOnAction(e -> {
            if (showConfirmation("Confirmation", "Voulez-vous vraiment réinitialiser le mot de passe de " + user.getPrenom() + " " + user.getNom() + " ?")) {

                btnResetPwd.setDisable(true);
                popupMessageLabel.setText("Génération et envoi du mot de passe en cours...");
                popupMessageLabel.setStyle("-fx-text-fill: blue;");

                new Thread(() -> {
                    try {
                        String plainToken = ConnexionService.generateRandomMdp(12);
                        userService.updateUserPassword(user.getId(), plainToken);

                        MailService.EnvoyerMail(
                                user.getEmail(),
                                "Réinitialisation de votre mot de passe",
                                "Bonjour " + user.getPrenom() + ",\n\nVotre mot de passe a été réinitialisé par un administrateur.\n\nVoici votre nouveau mot de passe temporaire : " + plainToken + "\n\nNous vous conseillons fortement de le modifier dès votre prochaine connexion."
                        );

                        Platform.runLater(() -> {
                            popupMessageLabel.setText("Mot de passe réinitialisé avec succès !");
                            popupMessageLabel.setStyle("-fx-text-fill: green;");
                            btnResetPwd.setDisable(false);
                            showAlert(Alert.AlertType.INFORMATION, "Succès", "Le nouveau mot de passe a été envoyé à " + user.getEmail());
                        });

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            popupMessageLabel.setText("Erreur lors de la réinitialisation.");
                            popupMessageLabel.setStyle("-fx-text-fill: red;");
                            btnResetPwd.setDisable(false);
                        });
                    }
                }).start();
            }
        });
    }

    /**
     * Ouvre une fenêtre modale de création d'utilisateur et envoie les identifiants par e-mail.
     * Liaison FXML : {@code addUserBtn}.
     *
     * @param actionEvent événement du bouton (non utilisé)
     */
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

        TextField adresseField = new TextField();
        adresseField.setPromptText("Adresse");
        adresseField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        TextField telField = new TextField();
        telField.setPromptText("Téléphone");
        telField.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #d1d5db; -fx-font-size: 14px; -fx-border-radius: 5;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "RH", "Employe");
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

        VBox layout = new VBox(10, message, nomField, prenomField, emailField, adresseField, telField, roleBox, posteField, popupMessageLabel, buttonBox);
        layout.setStyle("-fx-background-color: #f4f5f7; -fx-padding: 20; -fx-border-color: #d1d5db;");

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Ajouter un Utilisateur");
        popup.setScene(new Scene(layout, 350, 550));
        popup.setResizable(false);
        popup.show();

        button.setOnAction(e -> {
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String adresse = adresseField.getText().trim();
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
                    User newUser = new User(0, nom, prenom, email, plainToken, adresse, role, tel, poste, true);
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

    /**
     * Crée une carte d'événement affichée dans le panneau latéral.
     *
     * @param time libellé horaire ou date
     * @param title titre de l'événement
     * @param color couleur de fond (CSS)
     * @return conteneur vertical stylisé
     */
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

    /**
     * Affiche une boîte de dialogue d'information ou d'erreur.
     *
     * @param type type d'alerte JavaFX
     * @param title titre de la fenêtre
     * @param content message affiché
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Affiche une boîte de confirmation et retourne le choix de l'utilisateur.
     *
     * @param title titre de la fenêtre
     * @param content message affiché
     * @return {@code true} si l'utilisateur a confirmé (OK)
     */
    private boolean showConfirmation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }
}