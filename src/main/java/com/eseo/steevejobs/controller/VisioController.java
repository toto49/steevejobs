package com.eseo.steevejobs.controller;

import com.eseo.steevejobs.dao.UserDAO;
import com.eseo.steevejobs.model.Enum.VisioStatut;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.model.Visio;
import com.eseo.steevejobs.service.SessionService;
import com.eseo.steevejobs.service.WebSocketService;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisioController {

    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static VisioController activeInstance;
    private static Dotenv dotenv;
    private final UserDAO userDAO = new UserDAO();
    private boolean attenteTokenVisio = false;
    private String roomNameEnAttente = "";

    @FXML private TextField txtRoomName;
    @FXML private Button btnRejoindre;
    @FXML private Label lblStatut;
    @FXML private TextField txtPlanifRoomName;
    @FXML private DatePicker datePlanif;
    @FXML private Spinner<Integer> spinnerHeure;
    @FXML private Spinner<Integer> spinnerMinute;
    @FXML private Button btnPlanifier;
    private static final int ITEMS_PER_PAGE = 8;
    private static final double HAUTEUR_LIGNE_TABLE = 46;
    private final ContextMenu menuSuggestions = new ContextMenu();
    private final List<Integer> listeIdInvitesSelectionnes = new ArrayList<>();
    @FXML private TableColumn<Visio, String> colRoomName;
    @FXML private TableColumn<Visio, String> colStatut;
    @FXML private TableColumn<Visio, String> colDate;
    private final ObservableList<Visio> listeToutesReunions = FXCollections.observableArrayList();
    private final ObservableList<Visio> listeReunionsActives = FXCollections.observableArrayList();
    private final ObservableList<Visio> listeReunionsArchives = FXCollections.observableArrayList();
    @FXML private Button btnRejoindreSelection;
    private final ObservableList<User> tousLesEmployes = FXCollections.observableArrayList();
    @FXML
    private TabPane tabPaneReunions;
    @FXML
    private TableView<Visio> tableReunionsActives;
    @FXML
    private TableView<Visio> tableReunionsArchives;
    @FXML
    private TableColumn<Visio, String> colRoomNameArchive;
    @FXML
    private TableColumn<Visio, String> colStatutArchive;
    @FXML
    private TableColumn<Visio, String> colDateArchive;
    @FXML
    private Pagination paginationReunions;
    @FXML
    private TextField txtRechercheInvite;
    @FXML
    private FlowPane flowPaneInvitesBadges;

    public static VisioController getActiveInstance() {
        return activeInstance;
    }

    private static Dotenv getDotenv() {
        if (dotenv == null) dotenv = Dotenv.load();
        return dotenv;
    }

    @FXML
    public void initialize() {
        activeInstance = this;
        this.attenteTokenVisio = false;

        menuSuggestions.setStyle("-fx-background-color: #1F2937; -fx-border-color: #374151; -fx-background-radius: 6px; -fx-border-radius: 6px;");
        afficherStatut("Prêt.", false);

        initialiserTablesEtPagination();
        initialiserDateEtHeure();

        try {
            tousLesEmployes.setAll(userDAO.findActiveUsers());
        } catch (SQLException e) {
            afficherStatut("Impossible de charger la liste des employés.", true);
        }

        if (txtRechercheInvite != null) {
            txtRechercheInvite.textProperty().addListener((obs, oldText, newText) -> gererMenuSuggestionsEnTempsReel(newText));
            txtRechercheInvite.setOnAction(event -> ajouterInviteManuel());
        }

        rafraichirListeReunions();
    }

    public void couperController() {
        this.attenteTokenVisio = false;
        activeInstance = null;
    }

    private void initialiserTablesEtPagination() {
        if (tableReunionsActives != null) {
            colRoomName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoom_name()));
            colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatut() != null ? data.getValue().getStatut().getValeur() : ""));
            colDate.setCellValueFactory(data -> new SimpleStringProperty(formaterDate(data.getValue().getHeure_programmee())));
            configurerTableVisio(tableReunionsActives);
            ajouterColonneSuppression(tableReunionsActives);
            paginationReunions.setPageFactory(this::creerPageReunions);
        }

        if (tableReunionsArchives != null) {
            colRoomNameArchive.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRoom_name()));
            colStatutArchive.setCellValueFactory(data -> new SimpleStringProperty("Terminée"));
            colDateArchive.setCellValueFactory(data -> new SimpleStringProperty(formaterDate(data.getValue().getHeure_programmee())));
            configurerTableVisio(tableReunionsArchives);
            ajouterColonneSuppression(tableReunionsArchives);
            tableReunionsArchives.setItems(listeReunionsArchives);
        }
    }

    private void configurerTableVisio(TableView<Visio> table) {
        table.setFixedCellSize(HAUTEUR_LIGNE_TABLE);
        table.setPlaceholder(new Label("Aucune réunion à afficher."));
    }

    private void ajouterColonneSuppression(TableView<Visio> table) {
        TableColumn<Visio, Void> colSupprimer = new TableColumn<>("");
        colSupprimer.setPrefWidth(52);
        colSupprimer.setMinWidth(52);
        colSupprimer.setMaxWidth(52);
        colSupprimer.setSortable(false);
        colSupprimer.setResizable(false);
        colSupprimer.setCellFactory(col -> new TableCell<>() {
            private final Button btnSupprimer = new Button("🗑");

            {
                btnSupprimer.setOnAction(event -> {
                    Visio visio = getTableView().getItems().get(getIndex());
                    if (visio != null) {
                        confirmerEtSupprimerSalon(visio);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Visio visio = getTableView().getItems().get(getIndex());
                btnSupprimer.getStyleClass().removeAll("visio-btn-delete", "visio-btn-delete-disabled");

                if (estCreateurSalon(visio)) {
                    btnSupprimer.setDisable(false);
                    btnSupprimer.getStyleClass().add("visio-btn-delete");
                } else {
                    btnSupprimer.setDisable(true);
                    btnSupprimer.getStyleClass().add("visio-btn-delete-disabled");
                }

                setGraphic(btnSupprimer);
                setAlignment(Pos.CENTER);
            }
        });
        table.getColumns().add(colSupprimer);
    }

    private boolean estCreateurSalon(Visio visio) {
        User utilisateur = SessionService.getUtilisateurConnecte();
        return utilisateur != null
                && visio != null
                && visio.getCreateur_id() == utilisateur.getId();
    }

    private void confirmerEtSupprimerSalon(Visio visio) {
        if (!estCreateurSalon(visio)) {
            afficherStatut("Seul le créateur du salon peut le supprimer.", true);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Supprimer le salon");
        confirmation.setHeaderText("Supprimer « " + visio.getRoom_name() + " » ?");
        confirmation.setContentText("Cette action retirera le salon pour tous les participants.");

        Optional<ButtonType> choix = confirmation.showAndWait();
        if (choix.isEmpty() || choix.get() != ButtonType.OK) {
            return;
        }

        envoyerSuppressionSalon(visio.getRoom_name());
    }

    private void envoyerSuppressionSalon(String roomName) {
        JSONObject requete = new JSONObject();
        requete.put("type", "DELETE_VISIO");
        requete.put("roomName", roomName);
        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Suppression du salon en cours...", false);
    }

    public void recevoirSuppressionSalon(String message) {
        Platform.runLater(() -> {
            boolean succes = message.startsWith("✅");
            afficherStatut(message, !succes);
            if (succes) {
                rafraichirListeReunions();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Suppression impossible");
                alert.setHeaderText(null);
                alert.setContentText(message.replace("❌ ", ""));
                alert.showAndWait();
            }
        });
    }

    private String formaterDate(LocalDateTime heure) {
        return heure != null ? heure.format(DATE_HEURE_FMT) : "—";
    }

    private Node creerPageReunions(int pageIndex) {
        afficherPageReunions(pageIndex);
        return new VBox();
    }

    private void afficherPageReunions(int pageIndex) {
        if (tableReunionsActives == null) {
            return;
        }

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        if (fromIndex >= listeReunionsActives.size()) {
            tableReunionsActives.setItems(FXCollections.observableArrayList());
            return;
        }

        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, listeReunionsActives.size());
        tableReunionsActives.setItems(
                FXCollections.observableArrayList(listeReunionsActives.subList(fromIndex, toIndex))
        );
    }

    private void mettreAJourPagination() {
        int pageCount = (int) Math.ceil((double) listeReunionsActives.size() / ITEMS_PER_PAGE);
        if (pageCount == 0) {
            pageCount = 1;
        }

        int pageCourante = paginationReunions.getCurrentPageIndex();
        paginationReunions.setPageCount(pageCount);

        if (pageCourante >= pageCount) {
            pageCourante = 0;
            paginationReunions.setCurrentPageIndex(0);
        }

        afficherPageReunions(pageCourante);
    }


    private void gererMenuSuggestionsEnTempsReel(String saisie) {
        if (saisie == null || saisie.trim().isEmpty()) {
            menuSuggestions.hide();
            return;
        }

        String recherche = saisie.trim().toLowerCase();
        menuSuggestions.getItems().clear();

        for (User u : tousLesEmployes) {
            String nomComplet = (u.getPrenom() + " " + u.getNom());
            if (nomComplet.toLowerCase().contains(recherche)) {
                MenuItem item = new MenuItem(nomComplet + " — " + u.getPoste());
                item.setStyle("-fx-text-fill: #F9FAFB; -fx-font-family: 'Segoe UI'; -fx-padding: 6 12;");
                item.setOnAction(e -> {
                    creerBadgeInvite(u);
                    txtRechercheInvite.clear();
                    menuSuggestions.hide();
                });
                menuSuggestions.getItems().add(item);
            }
        }

        if (!menuSuggestions.getItems().isEmpty()) {
            if (!menuSuggestions.isShowing()) {
                menuSuggestions.show(txtRechercheInvite, javafx.geometry.Side.BOTTOM, 0, 2);
            }
        } else {
            menuSuggestions.hide();
        }
    }

    private void creerBadgeInvite(User u) {
        final int idEmploye = u.getId();
        final String nomAffichage = u.getPrenom() + " " + u.getNom();

        if (listeIdInvitesSelectionnes.contains(idEmploye)) return;
        listeIdInvitesSelectionnes.add(idEmploye);

        HBox badge = new HBox();
        badge.getStyleClass().add("user-badge");

        Label lblNom = new Label(nomAffichage);
        lblNom.getStyleClass().add("user-badge-text");

        Label lblSupprimer = new Label("❌");
        lblSupprimer.getStyleClass().add("user-badge-delete");

        lblSupprimer.setOnMouseClicked(event -> {
            flowPaneInvitesBadges.getChildren().remove(badge);
            listeIdInvitesSelectionnes.remove(Integer.valueOf(idEmploye));
        });

        badge.getChildren().addAll(lblNom, lblSupprimer);
        flowPaneInvitesBadges.getChildren().add(badge);
    }

    @FXML
    private void ajouterInviteManuel() {
        String recherche = txtRechercheInvite.getText().trim();
        if (recherche.isEmpty()) return;

        for (User u : tousLesEmployes) {
            String nomComplet = (u.getPrenom() + " " + u.getNom()).toLowerCase();
            if (nomComplet.contains(recherche.toLowerCase())) {
                creerBadgeInvite(u);
                txtRechercheInvite.clear();
                menuSuggestions.hide();
                return;
            }
        }
        afficherStatut("⚠️ Aucun collaborateur trouvé pour : " + recherche, true);
    }



    @FXML
    private void demanderConnexion() {
        String room = txtRoomName.getText().trim();
        if (room.isEmpty()) {
            afficherStatut("Veuillez renseigner un identifiant de salon.", true);
            return;
        }
        envoyerRequeteConnexion(room);
    }

    @FXML
    private void rejoindreSalonSelectionne() {
        Visio visioSelectionnee = tableReunionsActives.getSelectionModel().getSelectedItem();
        if (visioSelectionnee == null) {
            afficherStatut("Sélectionnez d'abord une réunion dans le tableau.", true);
            return;
        }
        envoyerRequeteConnexion(visioSelectionnee.getRoom_name());
    }

    private void envoyerRequeteConnexion(String roomName) {
        if (attenteTokenVisio) {
            afficherStatut("Connexion déjà en cours, patientez...", false);
            return;
        }

        User currentUser = SessionService.getUtilisateurConnecte();
        if (currentUser == null) {
            afficherStatut("Utilisateur non connecté.", true);
            return;
        }

        this.attenteTokenVisio = true;
        this.roomNameEnAttente = roomName;
        definirBoutonsConnexionDesactives(true);

        JSONObject requete = new JSONObject();
        requete.put("type", "REQUEST_VISIO_TOKEN");
        requete.put("roomName", roomName);
        requete.put("identity", construireIdentityLiveKit(currentUser));
        requete.put("displayName", construireNomAffichageLiveKit(currentUser));

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Connexion au serveur de visio en cours...", false);
    }

    private static String construireIdentityLiveKit(User user) {
        return String.valueOf(user.getId());
    }

    private static String construireNomAffichageLiveKit(User user) {
        return user.getPrenom() + " " + user.getNom();
    }

    private void definirBoutonsConnexionDesactives(boolean desactiver) {
        if (btnRejoindre != null) btnRejoindre.setDisable(desactiver);
        if (btnRejoindreSelection != null) btnRejoindreSelection.setDisable(desactiver);
    }

    @FXML
    private void planifierReunion() {
        String room = txtPlanifRoomName.getText().trim();
        if (room.isEmpty() || datePlanif.getValue() == null) {
            afficherStatut("Nom de salle et date obligatoires.", true);
            return;
        }

        Integer heure = lireValeurSpinner(spinnerHeure);
        Integer minute = lireValeurSpinner(spinnerMinute);
        if (heure == null || minute == null) {
            afficherStatut("Heure et minutes invalides.", true);
            return;
        }

        LocalDateTime heurePro = LocalDateTime.of(datePlanif.getValue(), LocalTime.of(heure, minute));

        JSONArray invitesArray = new JSONArray();
        for (int idInvite : listeIdInvitesSelectionnes) {
            invitesArray.put(idInvite);
        }

        JSONObject planifMsg = new JSONObject();
        planifMsg.put("type", "PLANIFY_VISIO");
        planifMsg.put("roomName", room);
        planifMsg.put("heureProgrammee", heurePro.toString());
        planifMsg.put("invites", invitesArray);

        WebSocketService.getInstance().envoyerMessageBrut(planifMsg.toString());
        afficherStatut("Enregistrement de la planification...", false);

        txtPlanifRoomName.clear();
        flowPaneInvitesBadges.getChildren().clear();
        listeIdInvitesSelectionnes.clear();
    }

    public void rafraichirListeReunions() {
        JSONObject msg = new JSONObject();
        msg.put("type", "GET_MY_VISIOS");
        WebSocketService.getInstance().envoyerMessageBrut(msg.toString());
    }
    public void recevoirTokenEtLancer(String tokenJWT, String roomNameServeur) {
        if (!attenteTokenVisio) return;
        this.attenteTokenVisio = false;

        String roomEffective = (roomNameEnAttente != null && !roomNameEnAttente.isBlank())
                ? roomNameEnAttente
                : roomNameServeur;
        if (roomEffective == null || roomEffective.isBlank()) {
            Platform.runLater(() -> {
                definirBoutonsConnexionDesactives(false);
                afficherStatut("Nom de salle introuvable pour ouvrir la visio.", true);
            });
            return;
        }

        Platform.runLater(() -> {
            definirBoutonsConnexionDesactives(false);
            try {
                String urlFrontNas = getDotenv().get("URL_FRONT_VISIO");
                String urlComplete = String.format(
                        "%s?token=%s&room=%s&t=%d",
                        urlFrontNas,
                        URLEncoder.encode(tokenJWT, StandardCharsets.UTF_8),
                        URLEncoder.encode(roomEffective, StandardCharsets.UTF_8),
                        System.currentTimeMillis()
                );
                System.out.println("Lancement visio : " + urlComplete);

                afficherStatut("Ouverture de la visio dans votre navigateur...", false);

                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", "\"" + urlComplete + "\""});
                } else {
                    Desktop.getDesktop().browse(new URI(urlComplete));
                }
            } catch (Exception e) {
                afficherStatut("Erreur : " + e.getMessage(), true);
            }
        });
    }

    public void recevoirErreurVisio(String messageErreur) {
        this.attenteTokenVisio = false;
        Platform.runLater(() -> {
            definirBoutonsConnexionDesactives(false);
            if (messageErreur.startsWith("✅")) {
                afficherStatut(messageErreur, false);
                rafraichirListeReunions();
            } else {
                afficherStatut(messageErreur, true);
            }
        });
    }

    public void recevoirListeReunions(JSONArray reunionsJson) {
        Platform.runLater(() -> {
            listeToutesReunions.clear();
            listeReunionsActives.clear();
            listeReunionsArchives.clear();

            for (int i = 0; i < reunionsJson.length(); i++) {
                JSONObject obj = reunionsJson.getJSONObject(i);

                Visio v = new Visio();
                v.setId(obj.getInt("id"));
                v.setRoom_name(obj.getString("roomName"));
                v.setCreateur_id(obj.optInt("createurId", -1));
                v.setStatut(VisioStatut.valueOf(obj.getString("statut")));

                String dateStr = obj.optString("heureProgrammee");
                if (!dateStr.isEmpty()) {
                    v.setHeure_programmee(LocalDateTime.parse(dateStr));
                }

                listeToutesReunions.add(v);
                if (v.getStatut() == VisioStatut.TERMINE) {
                    listeReunionsArchives.add(v);
                } else {
                    listeReunionsActives.add(v);
                }
            }
            mettreAJourPagination();
        });
    }


    private void initialiserDateEtHeure() {
        LocalDateTime maintenant = LocalDateTime.now().plusMinutes(15);
        if (datePlanif != null) datePlanif.setValue(maintenant.toLocalDate());
        if (spinnerHeure != null) {
            spinnerHeure.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, maintenant.getHour()));
            formaterSpinner(spinnerHeure);
        }
        if (spinnerMinute != null) {
            spinnerMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, maintenant.getMinute(), 1));
            formaterSpinner(spinnerMinute);
        }
    }

    private void formaterSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.getValueFactory().setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) return null;
                String chiffres = string.replaceAll("\\D", "");
                return chiffres.isEmpty() ? null : Integer.parseInt(chiffres);
            }
        });
    }

    private Integer lireValeurSpinner(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) return null;
        try {
            spinner.commitValue();
        } catch (Exception ignored) {
        }
        Integer value = spinner.getValue();
        if (value == null) return null;

        if (spinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
            if (value < intFactory.getMin() || value > intFactory.getMax()) return null;
        }
        return value;
    }

    private void afficherStatut(String message, boolean erreur) {
        if (lblStatut == null) return;
        lblStatut.setStyle(erreur ? "-fx-text-fill: #EF4444;" : "-fx-text-fill: #10B981;");
        lblStatut.setText(message);
    }
}