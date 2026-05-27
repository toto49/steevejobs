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
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
    @FXML private ListView<User> listEmployesDisponibles;
    @FXML private Button btnPlanifier;
    @FXML private TableView<Visio> tableReunions;
    @FXML private TableColumn<Visio, String> colRoomName;
    @FXML private TableColumn<Visio, String> colStatut;
    @FXML private TableColumn<Visio, String> colDate;
    @FXML private Button btnRejoindreSelection;

    private final ObservableList<Visio> listeReunionsData = FXCollections.observableArrayList();
    private final ObservableList<User> listeEmployesData = FXCollections.observableArrayList();

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

        afficherStatut("Prêt.", false);

        if (tableReunions != null) {
            colRoomName.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getRoom_name()));
            colStatut.setCellValueFactory(data -> {
                VisioStatut statut = data.getValue().getStatut();
                return new SimpleStringProperty(statut != null ? statut.getValeur() : "");
            });
            colDate.setCellValueFactory(data -> {
                LocalDateTime heure = data.getValue().getHeure_programmee();
                String texte = heure != null ? heure.format(DATE_HEURE_FMT) : "—";
                return new SimpleStringProperty(texte);
            });
            tableReunions.setItems(listeReunionsData);
            rafraichirListeReunions();
        }

        initialiserDateEtHeure();

        if (listEmployesDisponibles != null) {
            listEmployesDisponibles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listEmployesDisponibles.setItems(listeEmployesData);
            chargerListeEmployes();
        }
    }

    public void couperController() {
        this.attenteTokenVisio = false;
        activeInstance = null;
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
        Visio visioSelectionnee = tableReunions.getSelectionModel().getSelectedItem();
        if (visioSelectionnee == null) {
            afficherStatut("Sélectionnez d'abord une réunion dans le tableau.", true);
            return;
        }
        envoyerRequeteConnexion(visioSelectionnee.getRoom_name());
    }

    private void envoyerRequeteConnexion(String roomName) {
        this.attenteTokenVisio = true;
        this.roomNameEnAttente = roomName;
        User currentUser = SessionService.getUtilisateurConnecte();
        String nomUtilisateur = (currentUser != null) ? currentUser.getNom() : "Tom_Boudaud";

        JSONObject requete = new JSONObject();
        requete.put("type", "REQUEST_VISIO_TOKEN");
        requete.put("roomName", roomName);
        requete.put("identity", nomUtilisateur);

        WebSocketService.getInstance().envoyerMessageBrut(requete.toString());
        afficherStatut("Connexion au serveur de visio en cours...", false);
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
        for (User u : listEmployesDisponibles.getSelectionModel().getSelectedItems()) {
            invitesArray.put(u.getId());
        }

        JSONObject planifMsg = new JSONObject();
        planifMsg.put("type", "PLANIFY_VISIO");
        planifMsg.put("roomName", room);
        planifMsg.put("heureProgrammee", heurePro.toString());
        planifMsg.put("invites", invitesArray);

        WebSocketService.getInstance().envoyerMessageBrut(planifMsg.toString());
        afficherStatut("Enregistrement de la planification...", false);
    }

    public void rafraichirListeReunions() {
        JSONObject msg = new JSONObject();
        msg.put("type", "GET_MY_VISIOS");
        WebSocketService.getInstance().envoyerMessageBrut(msg.toString());
    }

    private void initialiserDateEtHeure() {
        LocalDateTime maintenant = LocalDateTime.now().plusMinutes(15);

        if (datePlanif != null) {
            datePlanif.setValue(maintenant.toLocalDate());
        }
        if (spinnerHeure != null) {
            spinnerHeure.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, maintenant.getHour()));
            formaterSpinner(spinnerHeure);
        }
        if (spinnerMinute != null) {
            spinnerMinute.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, maintenant.getMinute(), 1));
            formaterSpinner(spinnerMinute);
        }
    }

    private void formaterSpinner(Spinner<Integer> spinner) {
        spinner.setEditable(true);
        spinner.setMinWidth(130);
        spinner.setPrefWidth(130);
        spinner.getValueFactory().setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.isBlank()) {
                    return null;
                }
                String chiffres = string.replaceAll("\\D", "");
                if (chiffres.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(chiffres);
            }
        });
    }

    private Integer lireValeurSpinner(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return null;
        }
        try {
            spinner.commitValue();
        } catch (Exception ignored) {
        }
        Integer value = spinner.getValue();
        if (value == null) {
            return null;
        }
        SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
        if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intFactory) {
            if (value < intFactory.getMin() || value > intFactory.getMax()) {
                return null;
            }
        }
        return value;
    }

    private void chargerListeEmployes() {
        try {
            listeEmployesData.setAll(userDAO.findActiveUsers());
            listEmployesDisponibles.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getPrenom() + " " + user.getNom() + " — " + user.getPoste());
                    }
                }
            });
        } catch (SQLException e) {
            afficherStatut("Impossible de charger la liste des employés.", true);
        }
    }

    private void afficherStatut(String message, boolean erreur) {
        if (lblStatut == null) return;
        lblStatut.setStyle(erreur ? "-fx-text-fill: #E81123;" : "-fx-text-fill: #5882D6;");
        lblStatut.setText(message);
    }

    public void recevoirTokenEtLancer(String tokenJWT) {
        if (!attenteTokenVisio) return;
        this.attenteTokenVisio = false;

        Platform.runLater(() -> {
            try {
                String urlFrontNas = getDotenv().get("URL_FRONT_VISIO");
                String urlComplete = String.format(
                        "%s?token=%s&room=%s",
                        urlFrontNas,
                        URLEncoder.encode(tokenJWT, StandardCharsets.UTF_8),
                        URLEncoder.encode(roomNameEnAttente, StandardCharsets.UTF_8)
                );
                System.out.println("Lancement visio : " + urlComplete);

                afficherStatut("Ouverture de la visio dans votre navigateur...", false);

                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", urlComplete});
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
        Platform.runLater(() -> afficherStatut(messageErreur, true));
    }

    public void recevoirListeReunions(JSONArray reunionsJson) {
        Platform.runLater(() -> {
            listeReunionsData.clear();
            for (int i = 0; i < reunionsJson.length(); i++) {
                JSONObject obj = reunionsJson.getJSONObject(i);

                Visio v = new Visio();
                v.setId(obj.getInt("id"));
                v.setRoom_name(obj.getString("roomName"));
                v.setStatut(VisioStatut.valueOf(obj.getString("statut")));

                String dateStr = obj.optString("heureProgrammee");
                if (!dateStr.isEmpty()) {
                    v.setHeure_programmee(LocalDateTime.parse(dateStr));
                }
                listeReunionsData.add(v);
            }
        });
    }
}
